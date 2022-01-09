package main.utils.json.themes;

import main.constants.RobertifyTheme;
import main.utils.database.mongodb.cache.GuildsDBCache;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ThemesConfig extends AbstractGuildConfig {

    public RobertifyTheme getTheme(long gid) {
        if (!guildHasInfo(gid))
            throw new IllegalArgumentException("This guild doesn't have any information!");

        String theme;

        try {
            theme = getGuildObject(gid).getString(ThemesConfigField.THEME.toString());
        } catch (JSONException e) {
            update();
            theme = getGuildObject(gid).getString(ThemesConfigField.THEME.toString());
        }

        return RobertifyTheme.parse(theme.toLowerCase());
    }

    public void setTheme(long gid, RobertifyTheme theme) {
        if (!guildHasInfo(gid))
            throw new IllegalArgumentException("This guild doesn't have any information!");

        final var obj = getGuildObject(gid);
        obj.put(ThemesConfigField.THEME.toString(), theme.name().toLowerCase());
        getCache().updateGuild(obj, gid);
    }

    @Override
    public void update() {
        final JSONArray cacheArr = GuildsDBCache.getInstance().getCache();
        final List<JSONObject> objectsToUpdate = new ArrayList<>();

        boolean changesMade = false;
        for (var obj : cacheArr) {
            final var actualObj = (JSONObject) obj;

            if (!actualObj.has(ThemesConfigField.THEME.toString())) {
                changesMade = true;
                actualObj.put(ThemesConfigField.THEME.toString(), RobertifyTheme.GREEN.name().toLowerCase());
                objectsToUpdate.add(actualObj);
            }
        }

        if (changesMade) getCache().updateCacheObjects(objectsToUpdate);
    }
}
