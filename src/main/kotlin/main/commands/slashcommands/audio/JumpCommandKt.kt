package main.commands.slashcommands.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.client.player.IPlayer
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.length
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.GeneralUtilsKt.isInt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JumpCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "jump",
        description = "Skip the playing song by a given number of seconds",
        options = listOf(
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "seconds",
                description = "The number of seconds to skip in the song"
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = event.guild!!.selfMember.voiceState!!

        event.hook.sendWithEmbed {
            handleJump(
                selfVoiceState = selfVoiceState,
                memberVoiceState = memberVoiceState,
                input = event.getRequiredOption("seconds").asInt.toString()
            )
        }
            .setEphemeral(true)
            .queue()
    }

    private fun handleJump(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState,
        input: String
    ): MessageEmbed {

        val channelChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (channelChecks != null) return channelChecks

        val guild = selfVoiceState.guild
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val player = musicManager.player
        val track = player.playingTrack
            ?: return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING
            ).build()

        return doJump(
            guild = guild,
            jumper = memberVoiceState.member.user,
            input = input,
            player = player,
            track = track
        )
    }

    private fun doJump(
        guild: Guild,
        jumper: User,
        input: String,
        player: IPlayer,
        track: AudioTrack
    ): MessageEmbed {
        var time = if (input.isInt())
            input.toLong()
        else run {
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.JumpMessages.JUMP_INVALID_DURATION
            ).build()
        }

        if (time <= 0)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.JumpMessages.JUMP_DURATION_NEG_ZERO
            ).build()

        time = time.toDuration(DurationUnit.SECONDS).inWholeMilliseconds

        if (time > track.length - player.trackPosition)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.JumpMessages.JUMP_DURATION_GT_TIME_LEFT
            ).build()

        player.seekTo(player.trackPosition + time)
        LogUtilsKt(guild).sendLog(
            LogTypeKt.TRACK_JUMP,
            RobertifyLocaleMessageKt.JumpMessages.JUMPED_LOG,
            Pair("{user}", jumper.asMention),
            Pair("{duration}", time.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds.toString())
        )

        return RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RobertifyLocaleMessageKt.JumpMessages.JUMPED,
            Pair("{duration}", time.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds.toString())
        ).build()
    }
}