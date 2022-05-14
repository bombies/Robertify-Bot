package main.utils.database.mongodb.cache.redis;

import lombok.Getter;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static main.utils.database.mongodb.databases.GuildDB.getGuildDocument;

public class GuildRedisCache extends AbstractRedisCache {
    private final static Logger logger = LoggerFactory.getLogger(GuildRedisCache.class);

    @Getter
    private static GuildRedisCache instance;

    protected GuildRedisCache() {
        super("ROBERTIFY_GUILD", GuildDB.ins());
    }

    public static void initCache() {
        logger.debug("Instantiating new Guild cache");
        instance = new GuildRedisCache();
        AbstractGuildConfig.initCache();
    }

    public synchronized Object getField(long gid, GuildDB.Field field) {
        if (!guildHasInfo(gid))
            loadGuild(gid);
        return getGuildInfo(gid).get(field.toString());
    }

    public synchronized void setField(long gid, GuildDB.Field field, Object value) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        JSONObject guildInfo = getGuildInfo(gid);
        guildInfo.put(field.toString(), value);
        setex(gid, 3600, guildInfo);
    }

    public synchronized boolean hasField(long gid, GuildDB.Field field) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        return getGuildInfo(gid).has(field.toString());
    }

    public synchronized JSONObject getGuildInfo(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        final String guildInfo = get(gid);
        return guildInfo != null ? new JSONObject(guildInfo) : null;
    }

    public synchronized boolean guildHasInfo(long gid) {
        return get(gid) != null;
    }

    public synchronized void loadGuild(long gid) {
        loadGuild(gid, 0);
    }

    /**
     * Recursively attempt to load the guild into cache
     * @param gid The ID of the guild
     * @param attempt The recursive attempt
     */
    private synchronized void loadGuild(long gid, int attempt) {
        logger.debug("Attempting to load guild with ID: {}", gid);
        try {
            String guildJSON = getDocument(GuildDB.Field.GUILD_ID.toString(), gid);

            if (guildJSON != null) {
                setex(gid, 3600, new JSONObject(guildJSON));
                logger.debug("Loaded guild with ID: {}", gid);
            }
        } catch (NullPointerException e) {
            if (attempt == 2)
                return;

            logger.debug("Guild with ID {} didn't exist in the database. Attempting to add and reload.", gid);
            addDocument(getGuildDocument(gid));
            loadGuild(gid, ++attempt);
        }
    }

    public void loadAllGuilds() {
        logger.debug("Attempting to load all guilds");
        getCollection().find().forEach(document -> {
            JSONObject jsonObject = new JSONObject(document.toJson());
            final long gid = jsonObject.getLong(GuildDB.Field.GUILD_ID.toString());
            setex(gid, 3600, jsonObject);
            logger.debug("Loaded guild with id {}", gid);
        });
    }
}
