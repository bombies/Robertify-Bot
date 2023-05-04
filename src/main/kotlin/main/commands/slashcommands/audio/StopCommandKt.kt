package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.title
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.StopMessages
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class StopCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "stop",
        description = "Stop the music and clear the queue."
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbeds(handleStop(event.member!!)).queue()
    }

    fun handleStop(stopper: Member): MessageEmbed {
        val guild = stopper.guild
        val selfVoiceState = guild.selfMember.voiceState!!
        val memberVoiceState = stopper.voiceState!!
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val player = musicManager.player
        val scheduler = musicManager.scheduler
        val track = player.playingTrack

        return when {
            !selfVoiceState.inAudioChannel() -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            !memberVoiceState.inAudioChannel() -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

            memberVoiceState.channel!!.id != selfVoiceState.channel!!.id -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                Pair("{channel}", selfVoiceState.channel!!.asMention)
            ).build()

            track == null -> RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            else -> {
                musicManager.clear()
                player.stopTrack()
                logger.debug("Stopped track. Playing track: ${player.playingTrack?.title ?: "none"}")

                LogUtilsKt(guild).sendLog(
                    LogTypeKt.PLAYER_STOP,
                    StopMessages.STOPPED_LOG,
                    Pair("{user}", stopper.asMention)
                )
                scheduler.scheduleDisconnect(announceMsg = true)
                RobertifyEmbedUtilsKt.embedMessage(guild, StopMessages.STOPPED).build()
            }
        }

    }

    override val help: String
        get() = "Forces the bot to stop playing music and clear the queue if aslready playing music."
}