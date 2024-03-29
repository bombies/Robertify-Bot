package main.audiohandlers

import kotlinx.coroutines.*
import main.utils.json.guildconfig.GuildConfig
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class GuildDisconnectManager(private val guild: Guild) {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val executorService = Executors.newSingleThreadScheduledExecutor()
    }

    private var scheduledDisconnect: ScheduledFuture<*>? = null

    /**
     * Schedule the bot to disconnect according to your time requirements.
     * @param announceMsg Whether the bot should announce that it has disconnected due to activity or not.
     * @param duration The time the bot should wait before disconnect. Default to 5 minutes
     */
    fun scheduleDisconnect(duration: Duration = 5.minutes, announceMsg: Boolean = true) {
        logger.debug("${guild.name} | Starting scheduled disconnect")

        if (GuildConfig(guild).getTwentyFourSevenMode()) {
            logger.debug("${guild.name} | 24/7 mode is enabled, cancelling disconnect scheduling.")
            return
        }

        cancelDisconnect()
        logger.debug("${guild.name} | Cleared any previously scheduled disconnects")

        val job = executorService.schedule({
            val botVoiceState = guild.selfMember.voiceState
            if (botVoiceState == null || !botVoiceState.inAudioChannel())
                return@schedule

            RobertifyAudioManager
                .getMusicManager(guild)
                .scheduler
                .disconnect(announceMsg)
            logger.debug("${guild.name} | Bot disconnected.")
            scheduledDisconnect = null
        }, duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)

        scheduledDisconnect = job
        logger.debug("${guild.name} | successfully scheduled bot disconnect.")
    }

    /**
     *
     * Cancels the currently scheduled disconnect (if one is scheduled)
     * for the guild.
     */
    fun cancelDisconnect() {
        if (disconnectScheduled()) {
            logger.debug("${guild.name} | Cancelling disconnect.")
            scheduledDisconnect!!.cancel(false)
            scheduledDisconnect = null
        }
    }

    /***
     * This checks if a disconnect is scheduled for the current guild object.
     * @return True - There is a disconnect scheduled.
     * False - There are no disconnects scheduled.
     */
    fun disconnectScheduled(): Boolean = scheduledDisconnect != null
}