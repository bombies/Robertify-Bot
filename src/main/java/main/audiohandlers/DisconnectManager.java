package main.audiohandlers;

import lombok.val;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DisconnectManager {
    private final static Logger logger = LoggerFactory.getLogger(DisconnectManager.class);

    private static DisconnectManager instance;
    private final ScheduledExecutorService executorService;
    private final HashMap<Long, GuildDisconnectManager> guildDisconnects;

    private DisconnectManager() {
        this.executorService = Executors.newScheduledThreadPool(3);
        this.guildDisconnects = new HashMap<>();
    }

    public GuildDisconnectManager getGuildDisconnector(Guild guild) {
        if (guildDisconnects.containsKey(guild.getIdLong()))
            return guildDisconnects.get(guild.getIdLong());
        else {
            val guildDisconnectManager = new GuildDisconnectManager(guild, executorService);
            guildDisconnects.put(guild.getIdLong(), guildDisconnectManager);
            return guildDisconnectManager;
        }
    }

    public void destroyGuildDisconnector(Guild guild) {
        if (!guildDisconnects.containsKey(guild.getIdLong()))
            return;

        guildDisconnects.get(guild.getIdLong()).cancelDisconnect();
        guildDisconnects.remove(guild.getIdLong());
    }

    public static DisconnectManager getInstance(){
        if (instance == null)
            instance = new DisconnectManager();
        return instance;
    }


    protected static class GuildDisconnectManager {
        private final static Logger logger = LoggerFactory.getLogger(GuildDisconnectManager.class);

        private final Guild guild;
        private final ScheduledExecutorService executorService;
        private ScheduledFuture<?> scheduledDisconnect;

        public GuildDisconnectManager(Guild guild, ScheduledExecutorService executorService) {
            this.guild = guild;
            this.executorService = executorService;
        }

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

        public void scheduleDisconnect(boolean announceMsg) {
            this.scheduleDisconnect(announceMsg, 5L, TimeUnit.MINUTES);
        }

        public void cancelDisconnect() {
            if (disconnectScheduled()) {
                logger.debug("{} | Cancelling disconnect.", guild.getName());
                scheduledDisconnect.cancel(true);
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
}
