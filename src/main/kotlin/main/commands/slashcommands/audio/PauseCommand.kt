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

class PauseCommand : AbstractSlashCommand(
    SlashCommand(
        name = "pause",
        description = "Pause the song being currently played."
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = event.guild!!.selfMember.voiceState!!
        event.replyEmbed { handlePause(memberVoiceState, selfVoiceState) }.queue()
    }

    suspend fun handlePause(memberVoiceState: GuildVoiceState, selfVoiceState: GuildVoiceState): MessageEmbed {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks != null) return acChecks

        val guild = selfVoiceState.guild
        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player
        val logUtils = LogUtilsKt(guild)

        return if (player.paused) {
            player.pause(false)
            musicManager.isForcePaused = false
            logUtils.sendLog(
                LogType.PLAYER_RESUME,
                PauseMessages.RESUMED_LOG,
                Pair("{user}", memberVoiceState.member.asMention)
            )
            RobertifyEmbedUtils.embedMessage(guild, PauseMessages.RESUMED).build()
        } else {
            player.pause(true)
            musicManager.isForcePaused = true
            logUtils.sendLog(
                LogType.PLAYER_PAUSE,
                PauseMessages.PAUSED_LOG,
                Pair("{user}", memberVoiceState.member.asMention)
            )
            RobertifyEmbedUtils.embedMessage(guild, PauseMessages.PAUSED).build()
        }
    }

    override val help: String
        get() = "Pauses the song currently playing."
}