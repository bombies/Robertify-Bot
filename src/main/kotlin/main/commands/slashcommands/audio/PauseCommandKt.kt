package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.PauseMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PauseCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "pause",
        description = "Pause the song being currently played."
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = event.guild!!.selfMember.voiceState!!
        event.replyWithEmbed { handlePause(memberVoiceState, selfVoiceState) }.queue()
    }

    fun handlePause(memberVoiceState: GuildVoiceState, selfVoiceState: GuildVoiceState): MessageEmbed {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks != null) return acChecks

        val guild = selfVoiceState.guild
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val player = musicManager.player
        val logUtils = LogUtilsKt(guild)

        return if (player.isPaused) {
            player.isPaused = false
            musicManager.isForcePaused = false
            logUtils.sendLog(
                LogTypeKt.PLAYER_RESUME,
                PauseMessages.RESUMED_LOG,
                Pair("{user}", memberVoiceState.member.asMention)
            )
            RobertifyEmbedUtilsKt.embedMessage(guild, PauseMessages.RESUMED).build()
        } else {
            player.isPaused = true
            musicManager.isForcePaused = true
            logUtils.sendLog(
                LogTypeKt.PLAYER_PAUSE,
                PauseMessages.PAUSED_LOG,
                Pair("{user}", memberVoiceState.member.asMention)
            )
            RobertifyEmbedUtilsKt.embedMessage(guild, PauseMessages.PAUSED).build()
        }
    }

    override val help: String
        get() = "Pauses the song currently playing."
}