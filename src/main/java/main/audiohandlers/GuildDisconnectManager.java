package main.audiohandlers;

import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The manager that handles disconnects in a specific guild.
 */
public class GuildDisconnectManager {
    private final static Logger logger = LoggerFactory.getLogger(GuildDisconnectManager.class);

    private final Guild guild;
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledDisconnect;

    GuildDisconnectManager(Guild guild) {
        this.guild = guild;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Schedule the bot to disconnect according to your time requirements.
     * @param announceMsg Whether the bot should announce that it has disconnected due to activity or not.
     * @param time The time the bot should wait before disconnect
     * @param timeUnit The unit for the time
     */
    public void scheduleDisconnect(boolean announceMsg, long time, TimeUnit timeUnit) {
        final var botVoiceState = guild.getSelfMember().getVoiceState();
        if (botVoiceState == null || !botVoiceState.inAudioChannel())
            return;

        logger.debug("{} | Starting scheduled disconnect", guild.getName());

        if (new GuildConfig(guild).get247()) {
            logger.debug("{} | 24/7 mode is enabled, cancelling disconnect scheduling.", guild.getName());
            return;
        }

        cancelDisconnect();
        logger.debug("{} | Cleared any previously scheduled disconnects", guild.getName());

        scheduledDisconnect = executorService.schedule(() -> {
            RobertifyAudioManager.getInstance()
                    .getMusicManager(guild)
                    .getScheduler()
                    .disconnect(announceMsg);
            logger.debug("{} | Bot disconnected.", guild.getName());
            this.scheduledDisconnect = null;
        }, time, timeUnit);
        logger.debug("{} | Successfully scheduled bot disconnect.", guild.getName());
    }

    /**
     * The default method for scheduling a disconnect.
     * This bot will wait 5 minutes before disconnecting from the
     * voice channel it's in if it's in one.
     * @param announceMsg Whether the bot should announce that it has disconnected due to activity or not.
     */
    public void scheduleDisconnect(boolean announceMsg) {
        this.scheduleDisconnect(announceMsg, 5L, TimeUnit.MINUTES);
    }

    /**
     *
     * Cancels the currently scheduled disconnect (if one is scheduled)
     * for the guild.
     */
    public void cancelDisconnect() {
        if (disconnectScheduled()) {
            logger.debug("{} | Cancelling disconnect.", guild.getName());
            scheduledDisconnect.cancel(false);
            scheduledDisconnect = null;
        }
    }

    /***
     * This checks if a disconnect is scheduled for the current guild object.
     * @return True - There is a disconnect scheduled.
     * False - There are no disconnects scheduled.
     */
    public boolean disconnectScheduled() {
        return scheduledDisconnect != null;
    }
}
