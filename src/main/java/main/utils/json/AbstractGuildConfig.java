package main.utils.json;

import lombok.Getter;
import main.utils.database.mongodb.cache.GuildDBCache;
import main.utils.database.mongodb.cache.redis.GuildRedisCache;
import main.utils.database.mongodb.databases.GuildDB;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGuildConfig implements AbstractJSON {
    private final static Logger logger = LoggerFactory.getLogger(AbstractGuildConfig.class);
    @Getter
    private static GuildRedisCache cache;

    public abstract void update(long gid);

    public JSONObject getGuildObject(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);
        return cache.getGuildInfo(gid);
    }

    public static void initCache() {
        logger.debug("Instantiating Abstract Guild cache");
        cache = GuildRedisCache.getInstance();
    }

    public boolean guildHasInfo(long gid) {
        return cache.guildHasInfo(gid);
    }

    public void loadGuild(long gid) {
        cache.loadGuild(gid);
    }

    public void unloadGuild(long gid) {
        cache.unloadGuild(gid);
    }

    public GuildDB getDatabase() {
        return ((GuildDB) cache.getMongoDB());
    }
}
