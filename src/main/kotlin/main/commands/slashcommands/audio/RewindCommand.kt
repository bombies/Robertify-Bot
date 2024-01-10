package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.isStream
import main.audiohandlers.utils.title
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.JumpMessages
import main.utils.locale.messages.RewindMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class RewindCommand : AbstractSlashCommand(
    SlashCommand(
        name = "rewind",
        description = "Rewind the song by the seconds provided or all the way to the beginning.",
        options = listOf(
            CommandOption(
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
        event.hook.sendEmbed {
            handleRewind(
                memberVoiceState = event.member!!.voiceState!!,
                selfVoiceState = event.guild!!.selfMember.voiceState!!,
                time = event.getOption("seconds")?.asInt
            )
        }.queue()
    }

    suspend fun handleRewind(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        time: Int? = null
    ): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks != null) return acChecks

        val player = RobertifyAudioManager[guild].player!!
        val playingTrack = player.track!!

        if (playingTrack.isStream)
            return RobertifyEmbedUtils.embedMessage(guild, RewindMessages.CANT_REWIND_STREAM)
                .build()

        val logUtils = LogUtilsKt(guild)

        return if (time == null) {
            player.setPosition(0).subscribe()
            logUtils.sendLog(
                LogType.TRACK_REWIND,
                RewindMessages.REWIND_TO_BEGINNING_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", playingTrack.title),
                Pair("{author}", playingTrack.author)
            )
            RobertifyEmbedUtils.embedMessage(guild, RewindMessages.REWIND_TO_BEGINNING)
                .build()
        } else {
            if (time <= 0)
                return RobertifyEmbedUtils.embedMessage(
                    guild,
                    JumpMessages.JUMP_DURATION_NEG_ZERO
                ).build()

            val timeInMillis = time.toDuration(DurationUnit.SECONDS).inWholeMilliseconds

            if (timeInMillis > player.position)
                return RobertifyEmbedUtils.embedMessage(
                    guild,
                    RewindMessages.DURATION_GT_CURRENT_TIME
                ).build()

            player.setPosition(player.position - timeInMillis).subscribe()
            logUtils.sendLog(
                LogType.TRACK_REWIND,
                RewindMessages.REWOUND_BY_DURATION_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", playingTrack.title),
                Pair("{author}", playingTrack.author),
                Pair("{duration}", time.toString())
            )
            RobertifyEmbedUtils.embedMessage(
                guild,
                RewindMessages.REWOUND_BY_DURATION,
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