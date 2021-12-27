package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.GuildsDB;
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
        return getGuildInfo(gid).get(field.toString());
    }

    public void setField(long gid, GuildsDB.Field field, Object value) {
        JSONObject guildInfo = getGuildInfo(gid);
        guildInfo.put(field.toString(), value);
        updateCache(guildInfo, GuildsDB.Field.GUILD_ID, gid);
    }

    public JSONObject getGuildInfo(long gid) {
        return getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildsDB.Field.GUILD_ID, gid));    }
}
