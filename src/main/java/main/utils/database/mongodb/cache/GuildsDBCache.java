package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.databases.GuildsDB;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GuildsDBCache extends AbstractMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(GuildsDBCache.class);
    private final static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final static HashMap<Long, ScheduledFuture<?>> scheduledUnloads = new HashMap<>();

    @Getter
    private static GuildsDBCache instance;

    GuildsDBCache() {
        super(GuildsDB.ins());
        this.init();
        logger.debug("Done instantiating Guild cache");
    }

    public static void initCache() {
        logger.debug("Instantiating new Guild cache");
        instance = new GuildsDBCache();
        logger.debug("GUILD INFO CACHE = {}", instance.getCache());
        AbstractGuildConfig.initCache();
    }

    public synchronized Object getField(long gid, GuildsDB.Field field) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        delayUnload(gid);
        return getGuildInfo(gid).get(field.toString());
    }

    public synchronized void setField(long gid, GuildsDB.Field field, Object value) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        JSONObject guildInfo = getGuildInfo(gid);
        guildInfo.put(field.toString(), value);
        updateCache(guildInfo, GuildsDB.Field.GUILD_ID, gid);
        delayUnload(gid);
    }

    public synchronized boolean hasField(long gid, GuildsDB.Field field) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        delayUnload(gid);
        return getGuildInfo(gid).has(field.toString());
    }

    public synchronized JSONObject getGuildInfo(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        delayUnload(gid);
        try {
            return getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));
        } catch (JSONException | NullPointerException e) {
            return null;
        } catch (Exception e) {
            logger.error("An unexpected error occurred!", e);
            return null;
        }
    }

    public synchronized boolean guildHasInfo(long gid) {
        try {
            getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));
            return true;
        } catch (JSONException | NullPointerException e) {
            return false;
        }
    }

    public synchronized void loadGuild(long gid) {
        logger.debug("Attempting to load guild with ID: {}", gid);
        String guildJSON = getDocument(GuildsDB.Field.GUILD_ID.toString(), gid);

        if (guildJSON != null) {
            getCache().put(new JSONObject(guildJSON));
            logger.debug("Loaded guild with ID: {}", gid);

            if (!scheduledUnloads.containsKey(gid)) {
                logger.debug("Scheduling unload for guild with ID: {}", gid);
                ScheduledFuture<?> scheduledUnload = executorService.schedule(() -> {
                    unloadGuild(gid);
                    scheduledUnloads.remove(gid);
                }, 1, TimeUnit.HOURS);

                scheduledUnloads.put(gid, scheduledUnload);
            } else delayUnload(gid);
        }
    }

    public void delayUnload(long gid) {
        if (!scheduledUnloads.containsKey(gid))
            throw new IllegalArgumentException("There was no scheduled unload to delay for guild with ID: " + gid);

        logger.debug("Delaying unload for guild with ID: {}", gid);

        scheduledUnloads.get(gid).cancel(false);
        scheduledUnloads.remove(gid);

        ScheduledFuture<?> scheduledUnload = executorService.schedule(() -> {
            unloadGuild(gid);
        }, 1, TimeUnit.HOURS);

        scheduledUnloads.put(gid, scheduledUnload);
    }

    public synchronized void unloadGuild(long gid) {
        try {
            getCache().remove(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));
            logger.debug("Unloaded guild with ID: {}", gid);
        } catch (NullPointerException e) {
            logger.debug("Guild with ID {} could not be loaded since it wasn't found in the cache!", gid);
        }
    }
}
