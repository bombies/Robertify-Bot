package main.utils.json.autoplay;

import main.utils.json.AbstractGuildConfig;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

public class AutoPlayConfig extends AbstractGuildConfig {

    public boolean getStatus(long gid) {
        try {
            return getGuildObject(gid).getBoolean(Field.AUTOPLAY.name().toLowerCase());
        } catch (JSONException e) {
            update(gid);
            return getGuildObject(gid).getBoolean(Field.AUTOPLAY.name().toLowerCase());
        }
    }

    public void setStatus(long gid, boolean status) {
        JSONObject guildObject = getGuildObject(gid);
        guildObject.put(Field.AUTOPLAY.name().toLowerCase(), status);

        getCache().updateGuild(guildObject, gid);
    }

    @Override
    public void update(long gid) {
        JSONObject guildObject = getGuildObject(gid);

        if (!guildObject.has(Field.AUTOPLAY.name().toLowerCase()))
            guildObject.put(Field.AUTOPLAY.name().toLowerCase(), false);
        getCache().updateCache(String.valueOf(gid), Document.parse(guildObject.toString()));
    }

    public enum Field {
        AUTOPLAY
    }
}
