package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.databases.GuildsDB;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildsDBCache extends AbstractMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(GuildsDBCache.class);

    @Getter
    private static GuildsDBCache instance;

    GuildsDBCache() {
        super(GuildsDB.ins());
        this.init();
        updateCache();
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
            throw new NullPointerException("This guild doesn't have any info!");

        return getGuildInfo(gid).get(field.toString());
    }

    public synchronized void setField(long gid, GuildsDB.Field field, Object value) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any info!");

        JSONObject guildInfo = getGuildInfo(gid);
        guildInfo.put(field.toString(), value);
        updateCache(guildInfo, GuildsDB.Field.GUILD_ID, gid);
    }

    public synchronized JSONObject getGuildInfo(long gid) {
        if (!guildHasInfo(gid)) return null;
        return getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));
    }

    public synchronized boolean guildHasInfo(long gid) {
        try {
            getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));
            return true;
        } catch (JSONException | NullPointerException e) {
            return false;
        }
    }
}
