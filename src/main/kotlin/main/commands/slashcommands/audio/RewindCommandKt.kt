package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.isStream
import main.audiohandlers.utils.title
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class RewindCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "rewind",
        description = "Rewind the song by the seconds provided or all the way to the beginning.",
        options = listOf(
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "seconds",
                description = "The number of seconds to rewind the song by",
                required = false
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        event.hook.sendWithEmbed {
            handleRewind(
                memberVoiceState = event.member!!.voiceState!!,
                selfVoiceState = event.guild!!.selfMember.voiceState!!,
                time = event.getOption("seconds")?.asInt
            )
        }.queue()
    }

    fun handleRewind(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        time: Int? = null
    ): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks != null) return acChecks

        val player = RobertifyAudioManagerKt.getMusicManager(guild).player
        val playingTrack = player.playingTrack

        if (playingTrack.isStream)
            return RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.RewindMessages.CANT_REWIND_STREAM)
                .build()

        val logUtils = LogUtilsKt(guild)

        return if (time == null) {
            player.seekTo(0)
            logUtils.sendLog(
                LogTypeKt.TRACK_REWIND,
                RobertifyLocaleMessageKt.RewindMessages.REWIND_TO_BEGINNING_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", playingTrack.title),
                Pair("{author}", playingTrack.author)
            )
            RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.RewindMessages.REWIND_TO_BEGINNING)
                .build()
        } else {
            if (time <= 0)
                return RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.JumpMessages.JUMP_DURATION_NEG_ZERO
                ).build()

            val timeInMillis = time.toDuration(DurationUnit.SECONDS).inWholeMilliseconds

            if (timeInMillis > player.trackPosition)
                return RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.RewindMessages.DURATION_GT_CURRENT_TIME
                ).build()

            player.seekTo(player.trackPosition - timeInMillis)
            logUtils.sendLog(
                LogTypeKt.TRACK_REWIND,
                RobertifyLocaleMessageKt.RewindMessages.REWOUND_BY_DURATION_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", playingTrack.title),
                Pair("{author}", playingTrack.author),
                Pair("{duration}", time.toString())
            )
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.RewindMessages.REWOUND_BY_DURATION,
                Pair("{duration}", time.toString())
            ).build()
        }
    }

    override val help: String
        get() = """
                Rewind the song

                Usage: `/rewind` *(Rewinds the song to the beginning)*

                Usage: `/rewind <seconds_to_rewind>` *(Rewinds the song by a specific duration)*
                """
}