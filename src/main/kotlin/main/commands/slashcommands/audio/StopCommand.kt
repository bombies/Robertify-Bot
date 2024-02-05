package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.title
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.StopMessages
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.time.Duration.Companion.seconds

class StopCommand : AbstractSlashCommand(
    SlashCommand(
        name = "stop",
        description = "Stop the music and clear the queue."
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        event.hook.sendEmbed(event.guild) { handleStop(event.member!!) }.queue()
    }

    fun handleStop(stopper: Member): MessageEmbed {
        val guild = stopper.guild
        val selfVoiceState = guild.selfMember.voiceState!!
        val memberVoiceState = stopper.voiceState!!
        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player
        val scheduler = musicManager.scheduler
        val track = player?.track

        return when {
            !selfVoiceState.inAudioChannel() -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            !memberVoiceState.inAudioChannel() -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

            memberVoiceState.channel!!.id != selfVoiceState.channel!!.id -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                Pair("{channel}", selfVoiceState.channel!!.asMention)
            ).build()

            track == null -> RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

            else -> {
                musicManager.clear()
                player.stopTrack().subscribe()
                logger.debug("Stopped track. Playing track: ${player.track?.title ?: "none"}")

                LogUtilsKt(guild).sendLog(
                    LogType.PLAYER_STOP,
                    StopMessages.STOPPED_LOG,
                    Pair("{user}", stopper.asMention)
                )

                scheduler.scheduleDisconnect(announceMsg = true)
                return RobertifyEmbedUtils.embedMessage(guild, StopMessages.STOPPED).build()
            }
        }
    }

    override val help: String
        get() = "Forces the bot to stop playing music and clear the queue if aslready playing music."
}