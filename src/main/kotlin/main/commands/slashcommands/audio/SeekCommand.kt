package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.GeneralUtils.isNotNull
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.SeekMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SeekCommand : AbstractSlashCommand(
    SlashCommand(
        name = "seek",
        description = "Jump to a specific position in the current song",
        options = listOf(
            CommandOption(
                type = OptionType.INTEGER,
                name = "hours",
                description = "The amount of hours to seek"
            ),
            CommandOption(
                type = OptionType.INTEGER,
                name = "minutes",
                description = "The amount of minutes to seek"
            ),
            CommandOption(
                type = OptionType.INTEGER,
                name = "seconds",
                description = "The amount of seconds to seek"
            ),
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        event.hook.sendEmbed {
            handleSeek(
                memberVoiceState = event.member!!.voiceState!!,
                selfVoiceState = event.guild!!.selfMember.voiceState!!,
                hours = event.getRequiredOption("hours").asInt,
                minutes = event.getRequiredOption("minutes").asInt,
                seconds = event.getRequiredOption("seconds").asInt
            )
        }.queue()
    }

    private fun handleSeek(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        hours: Int,
        minutes: Int,
        seconds: Int
    ): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks.isNotNull()) return acChecks!!

        if (minutes < 0 || minutes > 59)
            return RobertifyEmbedUtils.embedMessage(guild, SeekMessages.INVALID_MINUTES)
                .build()

        if (seconds < 0 || seconds > 59)
            return RobertifyEmbedUtils.embedMessage(guild, SeekMessages.INVALID_SECONDS)
                .build()

        val seekDuration =
            hours.toDuration(DurationUnit.HOURS).inWholeMilliseconds + minutes.toDuration(DurationUnit.MINUTES).inWholeMilliseconds + seconds.toDuration(
                DurationUnit.SECONDS
            ).inWholeMilliseconds

        val player = RobertifyAudioManager[guild].player
        val playingTrack = player.playingTrack

        if (seekDuration > playingTrack.length)
            return RobertifyEmbedUtils.embedMessage(guild, SeekMessages.POS_GT_DURATION)
                .build()

        player.seekTo(seekDuration)
        val time =
            "${if (hours > 9) "$hours" else "0$hours"}:${if (minutes > 9) "$minutes" else "0$minutes"}:${if (seconds > 9) "$seconds" else "0$seconds"}"

        LogUtilsKt(guild).sendLog(
            LogType.TRACK_SEEK,
            SeekMessages.SOUGHT_LOG,
            Pair("{user}", memberVoiceState.member.asMention),
            Pair("{time}", time),
            Pair("{title}", playingTrack.title),
            Pair("{author}", playingTrack.author)
        )
        return RobertifyEmbedUtils.embedMessage(
            guild,
            SeekMessages.SOUGHT,
            Pair("{time}", time),
            Pair("{title}", playingTrack.title),
            Pair("{author}", playingTrack.author)
        ).build()
    }

    override val help: String
        get() = """
                Jump to a specific position in the current song

                Usage: `/seek <hours> <minutes> <seconds>`
                """
}