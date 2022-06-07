package main.utils.json.themes;

import main.constants.RobertifyTheme;
import main.utils.database.mongodb.cache.GuildDBCache;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ThemesConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;

    public ThemesConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public RobertifyTheme getTheme() {
        if (!guildHasInfo())
            loadGuild();

        String theme;

        try {
            theme = getGuildObject().getString(ThemesConfigField.THEME.toString());
        } catch (JSONException e) {
            update();
            theme = getGuildObject().getString(ThemesConfigField.THEME.toString());
        }

        return RobertifyTheme.parse(theme.toLowerCase());
    }

    public void setTheme(RobertifyTheme theme) {
        if (!guildHasInfo())
            loadGuild();

        final var obj = getGuildObject();
        obj.put(ThemesConfigField.THEME.toString(), theme.name().toLowerCase());
        getCache().updateGuild(obj, gid);
    }

    @Override
    public void update() {
        if (!guildHasInfo())
            loadGuild();

        final JSONArray cacheArr = GuildDBCache.getInstance().getCache();
        JSONObject object = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, gid));

        if (!object.has(ThemesConfigField.THEME.toString())) {
            object.put(ThemesConfigField.THEME.toString(), RobertifyTheme.GREEN.name().toLowerCase());
        }

        getCache().updateCache(String.valueOf(gid), Document.parse(object.toString()));
    }
}
