package main.utils.json.themes;

import main.constants.RobertifyTheme;
import main.utils.database.mongodb.cache.GuildDBCache;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ThemesConfig extends AbstractGuildConfig {

    public RobertifyTheme getTheme(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        String theme;

        try {
            theme = getGuildObject(gid).getString(ThemesConfigField.THEME.toString());
        } catch (JSONException e) {
            update(gid);
            theme = getGuildObject(gid).getString(ThemesConfigField.THEME.toString());
        }

        return RobertifyTheme.parse(theme.toLowerCase());
    }

    public void setTheme(long gid, RobertifyTheme theme) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        final var obj = getGuildObject(gid);
        obj.put(ThemesConfigField.THEME.toString(), theme.name().toLowerCase());
        getCache().updateGuild(obj, gid);
    }

    @Override
    public void update(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        final JSONArray cacheArr = GuildDBCache.getInstance().getCache();
        JSONObject object = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, gid));

        if (!object.has(ThemesConfigField.THEME.toString())) {
            object.put(ThemesConfigField.THEME.toString(), RobertifyTheme.GREEN.name().toLowerCase());
        }

        getCache().updateCache(String.valueOf(gid), Document.parse(object.toString()));
    }
}
