package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.PauseMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ResumeCommandKt : AbstractSlashCommandKt(CommandKt(
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
        val player = RobertifyAudioManagerKt[guild]
            .player

        if (!player.isPaused)
            return RobertifyEmbedUtilsKt.embedMessage(guild, PauseMessages.PLAYER_NOT_PAUSED).build()

        player.isPaused = false
        LogUtilsKt(guild).sendLog(
            LogTypeKt.PLAYER_RESUME,
            PauseMessages.RESUMED_LOG,
            Pair("{user}", memberVoiceState.member.asMention)
        )
        return RobertifyEmbedUtilsKt.embedMessage(guild, PauseMessages.RESUMED).build()
    }

    override val help: String
        get() = """
                Resumes the currently playing song if paused

                Usage: `/resume`"""
}