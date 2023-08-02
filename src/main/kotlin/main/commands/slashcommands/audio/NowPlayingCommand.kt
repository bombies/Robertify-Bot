package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.*
import main.constants.Toggle
import main.utils.GeneralUtils
import main.utils.GeneralUtils.isUrl
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageBuilderException
import main.utils.api.robertify.imagebuilders.builders.NowPlayingImageBuilder
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.themes.ThemesConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.NowPlayingMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload

class NowPlayingCommand : AbstractSlashCommand(
    SlashCommand(
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
        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player
        val track = player.playingTrack

        val embed: MessageEmbed? = when {
            !selfVoiceState.inAudioChannel() -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            !memberVoiceState.inAudioChannel() -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

            memberVoiceState.channel != selfVoiceState.channel ->
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                ).build()

            track == null -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            else -> null
        }

        if (embed != null) {
            event.hook.sendEmbed(guild) { embed }
                .queue()
        } else {
            val sendBackupEmbed: suspend () -> Unit = {
                event.hook.sendEmbed(guild) {
                    getNowPlayingEmbed(guild, event.channel.asGuildMessageChannel(), selfVoiceState, memberVoiceState)
                }.queue()
            }

            try {
                val defaultImage = ThemesConfig(guild).getTheme().nowPlayingBanner
                val builder = NowPlayingImageBuilder(
                    title = track!!.title,
                    artistName = track.author,
                    albumImage = track.artworkUrl
                        ?: defaultImage
                )

                val image = if (!track.isStream)
                    builder.copy(
                        duration = track.length,
                        currentTime = player.position,
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
                            AbstractImageBuilder.RANDOM_FILE_NAME
                        )
                    ).queue()
            } catch (e: ImageBuilderException) {
                sendBackupEmbed()
            }
        }
    }

    private suspend fun getNowPlayingEmbed(
        guild: Guild,
        channel: GuildMessageChannel,
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed {
        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player
        val track = player.playingTrack

        val embed: MessageEmbed? = when {
            !selfVoiceState.inAudioChannel() -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            !memberVoiceState.inAudioChannel() -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

            memberVoiceState.channel != selfVoiceState.channel ->
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                ).build()

            track == null -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            else -> null
        }

        if (embed != null)
            return embed

        val progress = player.position.toDouble() / track!!.length
        val filters = player.filters
        val requester = musicManager.scheduler.findRequester(track.identifier)
        val localeManager = LocaleManager[guild]
        val embedBuilder = RobertifyEmbedUtils.embedMessageWithTitle(
            guild, localeManager.getMessage(
                NowPlayingMessages.NP_EMBED_TITLE,
                Pair("{title}", track.title),
                Pair("{author}", track.author)
            ),
            "${
                if (TogglesConfig(guild).getToggle(Toggle.SHOW_REQUESTER) && requester != null) {
                    "\n\n${
                        localeManager.getMessage(
                            NowPlayingMessages.NP_REQUESTER,
                            Pair("{requester}", requester.toString())
                        )
                    }"
                } else {
                    ""
                }
            }\n\n" +
                    "${if (track.isStream) "" else "`[0:00]`"}${
                        GeneralUtils.progressBar(
                            guild,
                            channel,
                            progress,
                            GeneralUtils.ProgressBar.DURATION
                        )
                    }" +
                    "${if (track.isStream) "" else "`[${GeneralUtils.formatTime(track.length)}]`"}\n\n" +
                    "${
                        if (track.isStream) localeManager.getMessage(NowPlayingMessages.NP_LIVESTREAM) else localeManager.getMessage(
                            NowPlayingMessages.NP_TIME_LEFT,
                            Pair(
                                "{time}",
                                GeneralUtils.formatTime(track.length - player.position)
                            )
                        )
                    }\n" +
                    "\n🔇 ${
                        GeneralUtils.progressBar(
                            guild,
                            channel,
                            filters.volume?.toDouble() ?: 0.0,
                            GeneralUtils.ProgressBar.FILL
                        )
                    } 🔊"
        )

        if (track.artworkUrl != null)
            embedBuilder.setImage(track.artworkUrl)

        embedBuilder.setAuthor(
            localeManager.getMessage(NowPlayingMessages.NP_AUTHOR),
            if (track.uri.isUrl()) track.uri else null,
            ThemesConfig(guild).getTheme().transparent
        )
        return embedBuilder.build()
    }

    override val help: String
        get() = "Displays the song that is currently playing"
}