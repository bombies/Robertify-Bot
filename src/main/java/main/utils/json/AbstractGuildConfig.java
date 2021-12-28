package main.utils.json;

import lombok.Getter;
import main.utils.database.mongodb.GuildsDB;
import main.utils.database.mongodb.cache.GuildsDBCache;
import org.json.JSONObject;

public abstract class AbstractGuildConfig implements AbstractJSON {
    @Getter
    private static final GuildsDBCache cache = GuildsDBCache.getInstance();

    protected abstract void update();

    public JSONObject getGuildObject(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        return cache.getCache().getJSONObject(getIndexOfObjectInArray(cache.getCache(), GuildsDB.Field.GUILD_ID, gid));
    }

    public boolean guildHasInfo(long gid) {
        return cache.guildHasInfo(gid);
    }
}
