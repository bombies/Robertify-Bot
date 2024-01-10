package main.commands.slashcommands.audio

import dev.arbjerg.lavalink.client.LavalinkPlayer
import dev.arbjerg.lavalink.client.protocol.Track
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.length
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.GeneralUtils.isInt
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.JumpMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JumpCommand : AbstractSlashCommand(
    SlashCommand(
        name = "jump",
        description = "Skip the playing song by a given number of seconds",
        options = listOf(
            CommandOption(
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

        event.hook.sendEmbed {
            handleJump(
                selfVoiceState = selfVoiceState,
                memberVoiceState = memberVoiceState,
                input = event.getRequiredOption("seconds").asInt.toString()
            )
        }
            .setEphemeral(true)
            .queue()
    }

    private suspend fun handleJump(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState,
        input: String
    ): MessageEmbed {

        val channelChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (channelChecks != null) return channelChecks

        val guild = selfVoiceState.guild
        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player!!
        val track = player.track
            ?: return RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

        return doJump(
            guild = guild,
            jumper = memberVoiceState.member.user,
            input = input,
            player = player,
            track = track
        )
    }

    private suspend fun doJump(
        guild: Guild,
        jumper: User,
        input: String,
        player: LavalinkPlayer,
        track: Track
    ): MessageEmbed {
        var time = if (input.isInt())
            input.toLong()
        else run {
            return RobertifyEmbedUtils.embedMessage(
                guild,
                JumpMessages.JUMP_INVALID_DURATION
            ).build()
        }

        if (time <= 0)
            return RobertifyEmbedUtils.embedMessage(
                guild,
                JumpMessages.JUMP_DURATION_NEG_ZERO
            ).build()

        time = time.toDuration(DurationUnit.SECONDS).inWholeMilliseconds

        if (time > track.length - player.position)
            return RobertifyEmbedUtils.embedMessage(
                guild,
                JumpMessages.JUMP_DURATION_GT_TIME_LEFT
            ).build()

        player.setPosition(player.position + time)
        LogUtilsKt(guild).sendLog(
            LogType.TRACK_JUMP,
            JumpMessages.JUMPED_LOG,
            Pair("{user}", jumper.asMention),
            Pair("{duration}", time.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds.toString())
        )

        return RobertifyEmbedUtils.embedMessage(
            guild,
            JumpMessages.JUMPED,
            Pair("{duration}", time.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds.toString())
        ).build()
    }
}