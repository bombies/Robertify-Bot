package main.utils.json.toggles;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.utils.database.mongodb.GuildsDB;
import main.utils.database.mongodb.cache.GuildsDBCache;
import main.utils.json.AbstractGuildConfig;
import main.utils.json.AbstractJSON;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class TogglesConfig extends AbstractGuildConfig {
    public boolean getToggle(Guild guild, Toggles toggle) {
        if (!guildHasInfo(guild.getIdLong()))
            throw new IllegalArgumentException("This guild doesn't have any information!");

        return getTogglesObject(guild.getIdLong()).getBoolean(toggle.toString());
    }

    public HashMap<String, Boolean> getDJToggles(Guild g) {
        final HashMap<String, Boolean> ret = new HashMap<>();
        final var obj = getTogglesObject(g.getIdLong())
                .getJSONObject(GuildsDB.Field.TOGGLES_DJ.toString());

        for (String key : obj.keySet())
            ret.put(key, false);

        ret.replaceAll((k, v) -> obj.getBoolean(k));

        return ret;
    }

    public boolean getDJToggle(Guild g, ICommand cmd) {
        final var djToggles = getDJToggles(g);

        if (!djToggles.containsKey(cmd.getName()))
            throw new NullPointerException("Invalid command passed!");

        return djToggles.get(cmd.getName().toLowerCase());
    }

    public void setToggle(Guild guild, Toggles toggle, boolean val) {
        final var obj = new GuildConfig().getGuildObject(guild.getIdLong());
        obj.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString()).put(toggle.toString(), val);
        getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, guild.getIdLong());
    }

    public void setDJToggle(Guild guild, ICommand command, boolean val) {
        final var obj = new GuildConfig().getGuildObject(guild.getIdLong());
        obj.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString())
                .getJSONObject(GuildsDB.Field.TOGGLES_DJ.toString())
                .put(command.getName(), val);
        getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, guild.getIdLong());
    }

    public boolean isDJToggleSet(Guild guild, ICommand cmd) {
        return getDJToggles(guild).containsKey(cmd.getName().toLowerCase());
    }

    public boolean isDJToggleSet(Guild guild, String cmd) {
        return getDJToggles(guild).containsKey(cmd.toLowerCase());
    }

    private JSONObject getTogglesObject(long gid) {
        return getGuildObject(gid).getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString());
    }

    @Override
    public void update() {
        JSONArray cacheArr = getCache().getCache();

        for (int i = 0; i < cacheArr.length(); i++) {
            final JSONObject jsonObject = cacheArr.getJSONObject(i);
            boolean changesMade = false;

            for (Toggles toggle : Toggles.values())
                try {
                    JSONObject toggleObj = jsonObject.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString());

                    if (!toggleObj.has(toggle.toString()))
                        changesMade = true;
                        switch (toggle) {
                            case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> toggleObj.put(toggle.toString(), false);
                            default -> toggleObj.put(toggle.toString(), true);
                        }

                    if (!toggleObj.has(Toggles.TogglesConfigField.DJ_TOGGLES.toString())) {
                        var djTogglesObj = new JSONObject();

                        for (ICommand musicCommand : new CommandManager(new EventWaiter()).getMusicCommands())
                            djTogglesObj.put(musicCommand.getName().toLowerCase(), false);

                        toggleObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
                    } else {
                        var djTogglesObj = toggleObj.getJSONObject(Toggles.TogglesConfigField.DJ_TOGGLES.toString());

                        for (ICommand musicCommand : new CommandManager(new EventWaiter()).getMusicCommands())
                            if (!djTogglesObj.has(musicCommand.getName()))
                                djTogglesObj.put(musicCommand.getName(), false);

                        toggleObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
                    }
                } catch (JSONException e) {
                    for (Toggles errToggles : Toggles.values())
                        switch (errToggles) {
                            case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> jsonObject.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString())
                                    .put(errToggles.toString(), false);
                            default -> jsonObject.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString())
                                    .put(errToggles.toString(), true);
                        }
                }

            if (changesMade) getCache().updateCache(Document.parse(jsonObject.toString()));
        }
    }

}
