package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.PauseMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ResumeCommand : AbstractSlashCommand(SlashCommand(
    name = "resume",
    description = "Resume the currently paused track."
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = event.guild!!.selfMember.voiceState!!
        event.replyEmbed { handleResume(memberVoiceState, selfVoiceState) }.queue()
    }

    private fun handleResume(memberVoiceState: GuildVoiceState, selfVoiceState: GuildVoiceState): MessageEmbed {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks != null) return acChecks
        val guild = selfVoiceState.guild
        val player = RobertifyAudioManager[guild]
            .player!!

        if (!player.paused)
            return RobertifyEmbedUtils.embedMessage(guild, PauseMessages.PLAYER_NOT_PAUSED).build()

        player.setPaused(false).build()
        LogUtilsKt(guild).sendLog(
            LogType.PLAYER_RESUME,
            PauseMessages.RESUMED_LOG,
            Pair("{user}", memberVoiceState.member.asMention)
        )
        return RobertifyEmbedUtils.embedMessage(guild, PauseMessages.RESUMED).build()
    }

    override val help: String
        get() = """
                Resumes the currently playing song if paused

                Usage: `/resume`"""
}