package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.GeneralUtilsKt.isNotNull
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.SeekMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SeekCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "seek",
        description = "Jump to a specific position in the current song",
        options = listOf(
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "hours",
                description = "The amount of hours to seek"
            ),
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "minutes",
                description = "The amount of minutes to seek"
            ),
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "seconds",
                description = "The amount of seconds to seek"
            ),
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        event.hook.sendWithEmbed {
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
            return RobertifyEmbedUtilsKt.embedMessage(guild, SeekMessages.INVALID_MINUTES)
                .build()

        if (seconds < 0 || seconds > 59)
            return RobertifyEmbedUtilsKt.embedMessage(guild, SeekMessages.INVALID_SECONDS)
                .build()

        val seekDuration =
            hours.toDuration(DurationUnit.HOURS).inWholeMilliseconds + minutes.toDuration(DurationUnit.MINUTES).inWholeMilliseconds + seconds.toDuration(
                DurationUnit.SECONDS
            ).inWholeMilliseconds

        val player = RobertifyAudioManagerKt.getMusicManager(guild).player
        val playingTrack = player.playingTrack

        if (seekDuration > playingTrack.length)
            return RobertifyEmbedUtilsKt.embedMessage(guild, SeekMessages.POS_GT_DURATION)
                .build()

        player.seekTo(seekDuration)
        val time =
            "${if (hours > 9) "$hours" else "0$hours"}:${if (minutes > 9) "$minutes" else "0$minutes"}:${if (seconds > 9) "$seconds" else "0$seconds"}"

        LogUtilsKt(guild).sendLog(
            LogTypeKt.TRACK_SEEK,
            SeekMessages.SOUGHT_LOG,
            Pair("{user}", memberVoiceState.member.asMention),
            Pair("{time}", time),
            Pair("{title}", playingTrack.title),
            Pair("{author}", playingTrack.author)
        )
        return RobertifyEmbedUtilsKt.embedMessage(
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