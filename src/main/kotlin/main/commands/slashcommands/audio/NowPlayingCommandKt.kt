package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.*
import main.constants.ToggleKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.Companion.isUrl
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.api.robertify.imagebuilders.AbstractImageBuilderKt
import main.utils.api.robertify.imagebuilders.ImageBuilderExceptionKt
import main.utils.api.robertify.imagebuilders.builders.NowPlayingImageBuilderKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload

class NowPlayingCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "nowplaying",
        description = "See the song that is currently being played."
    )
) {

    companion object {
        val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        val guild = event.guild!!
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val player = musicManager.player
        val track = player.playingTrack

        val embed: MessageEmbed? = when {
            !selfVoiceState.inAudioChannel() -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING
            ).build()

            !memberVoiceState.inAudioChannel() -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

            memberVoiceState.channel != selfVoiceState.channel ->
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                ).build()

            track == null -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING
            ).build()

            else -> null
        }

        if (embed != null) {
            event.hook.sendWithEmbed(guild) { embed }
                .queue()
        } else {
            val sendBackupEmbed: () -> Unit = {
                event.hook.sendWithEmbed(guild) {
                    getNowPlayingEmbed(guild, event.channel.asGuildMessageChannel(), selfVoiceState, memberVoiceState)
                }.queue()
            }

            try {
                val defaultImage = ThemesConfigKt(guild).theme.nowPlayingBanner
                val builder = NowPlayingImageBuilderKt(
                    title = track!!.title,
                    artistName = track.author,
                    albumImage = if (track is MirroringAudioTrack) track.artworkURL
                        ?: defaultImage else defaultImage
                )

                val image = if (!track.isStream)
                    builder.copy(
                        duration = track.length,
                        currentTime = player.trackPosition,
                        isLiveStream = false
                    ).build()
                else builder.copy(
                    isLiveStream = false
                ).build()

                if (image == null)
                    sendBackupEmbed()
                else
                    event.hook.sendFiles(
                        FileUpload.fromData(
                            image,
                            AbstractImageBuilderKt.RANDOM_FILE_NAME
                        )
                    ).queue()
            } catch (e: ImageBuilderExceptionKt) {
                sendBackupEmbed()
            }
        }
    }

    private fun getNowPlayingEmbed(
        guild: Guild,
        channel: GuildMessageChannel,
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed {
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val player = musicManager.player
        val track = player.playingTrack

        val embed: MessageEmbed? = when {
            !selfVoiceState.inAudioChannel() -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING
            ).build()

            !memberVoiceState.inAudioChannel() -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

            memberVoiceState.channel != selfVoiceState.channel ->
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                ).build()

            track == null -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING
            ).build()

            else -> null
        }

        if (embed != null)
            return embed

        val progress = player.trackPosition.toDouble() / track!!.length
        val filters = player.filters
        val requester = musicManager.scheduler.findRequester(track.identifier)
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        val embedBuilder = RobertifyEmbedUtilsKt.embedMessageWithTitle(
            guild, localeManager.getMessage(
                RobertifyLocaleMessageKt.NowPlayingMessages.NP_EMBED_TITLE,
                Pair("{title}", track.title),
                Pair("{author}", track.author)
            ),
            "${
                if (TogglesConfigKt(guild).getToggle(ToggleKt.SHOW_REQUESTER) && requester != null) {
                    "\n\n${
                        localeManager.getMessage(
                            RobertifyLocaleMessageKt.NowPlayingMessages.NP_REQUESTER,
                            Pair("{requester}", requester.toString())
                        )
                    }"
                } else {
                    ""
                }
            }\n\n" +
                    "${if (track.isStream) "" else "`[0:00]`"}${
                        GeneralUtilsKt.progressBar(
                            guild,
                            channel,
                            progress,
                            GeneralUtilsKt.Companion.ProgressBar.DURATION
                        )
                    }" +
                    "${if (track.isStream) "" else "`[${GeneralUtilsKt.formatTime(track.length)}]`"}\n\n" +
                    "${
                        if (track.isStream) localeManager.getMessage(RobertifyLocaleMessageKt.NowPlayingMessages.NP_LIVESTREAM) else localeManager.getMessage(
                            RobertifyLocaleMessageKt.NowPlayingMessages.NP_TIME_LEFT,
                            Pair(
                                "{time}",
                                GeneralUtilsKt.formatTime(track.length - player.trackPosition)
                            )
                        )
                    }\n" +
                    "\n🔇 ${
                        GeneralUtilsKt.progressBar(
                            guild,
                            channel,
                            filters.volume.toDouble() ?: 0.0,
                            GeneralUtilsKt.Companion.ProgressBar.FILL
                        )
                    } 🔊"
        )

        if (track is MirroringAudioTrack)
            embedBuilder.setImage(track.artworkURL)

        embedBuilder.setAuthor(
            localeManager.getMessage(RobertifyLocaleMessageKt.NowPlayingMessages.NP_AUTHOR),
            if (track.uri.isUrl()) track.uri else null,
            ThemesConfigKt(guild).theme.transparent
        )
        return embedBuilder.build()
    }

    override val help: String
        get() = "Displays the song that is currently playing"
}