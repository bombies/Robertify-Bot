package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.GuildsDB;
import org.json.JSONException;
import org.json.JSONObject;

public class GuildsDBCache extends AbstractMongoCache {
    @Getter
    private static GuildsDBCache instance;

    GuildsDBCache() {
        super(GuildsDB.ins());
        this.init();
    }

    public static void initCache() {
        instance = new GuildsDBCache();
    }

    public Object getField(long gid, GuildsDB.Field field) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any info!");

        return getGuildInfo(gid).get(field.toString());
    }

    public void setField(long gid, GuildsDB.Field field, Object value) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any info!");

        JSONObject guildInfo = getGuildInfo(gid);
        guildInfo.put(field.toString(), value);
        updateCache(guildInfo, GuildsDB.Field.GUILD_ID, gid);
    }

    public JSONObject getGuildInfo(long gid) {
        if (!guildHasInfo(gid)) return null;
        return getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));
    }

    public boolean guildHasInfo(long gid) {
        try {
            getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));
            return true;
        } catch (JSONException | NullPointerException e) {
            return false;
        }
    }
}
