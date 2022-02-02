package main.utils.json;

import lombok.Getter;
import main.utils.database.mongodb.cache.GuildsDBCache;
import main.utils.database.mongodb.databases.GuildsDB;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGuildConfig implements AbstractJSON {
    private final static Logger logger = LoggerFactory.getLogger(AbstractGuildConfig.class);
    @Getter
    private static GuildsDBCache cache;

    public abstract void update(long gid);

    public JSONObject getGuildObject(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        return cache.getCache().getJSONObject(getIndexOfObjectInArray(cache.getCache(), GuildsDB.Field.GUILD_ID, gid));
    }

    public static void initCache() {
        logger.debug("Instantiating Abstract Guild cache");
        cache = GuildsDBCache.getInstance();
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

    public GuildsDB getDatabase() {
        return ((GuildsDB) cache.getMongoDB());
    }
}
