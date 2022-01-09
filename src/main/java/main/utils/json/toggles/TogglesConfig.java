package main.utils.json.toggles;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.constants.Toggles;
import main.utils.database.mongodb.GuildsDB;
import main.utils.database.mongodb.cache.GuildsDBCache;
import main.utils.json.AbstractGuildConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TogglesConfig extends AbstractGuildConfig {
    public boolean getToggle(Guild guild, Toggles toggle) {
        if (!guildHasInfo(guild.getIdLong()))
            throw new IllegalArgumentException("This guild doesn't have any information!");

        try {
            return getTogglesObject(guild.getIdLong()).getBoolean(toggle.toString());
        } catch (JSONException e) {
            update();
            return true;
        }
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

        if (!djToggles.containsKey(cmd.getName())) {
            if (new CommandManager(new EventWaiter()).getMiscCommands().contains(cmd)) {
                setDJToggle(g, cmd, false);
                return false;
            } else
                throw new NullPointerException("Invalid command passed! [Command: "+cmd.getName()+"]");
        }

        return djToggles.get(cmd.getName().toLowerCase());
    }

    public void setToggle(Guild guild, Toggles toggle, boolean val) {
        final var obj = getGuildObject(guild.getIdLong());
        obj.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString()).put(toggle.toString(), val);
        getCache().updateGuild(obj, guild.getIdLong());
    }

    public void setDJToggle(Guild guild, ICommand command, boolean val) {
        final var obj = getGuildObject(guild.getIdLong());
        obj.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString())
                .getJSONObject(GuildsDB.Field.TOGGLES_DJ.toString())
                .put(command.getName(), val);
        getCache().updateGuild(obj, guild.getIdLong());
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
        JSONArray cacheArr = GuildsDBCache.getInstance().getCache();
        final List<JSONObject> objectsToUpdate = new ArrayList<>();
        boolean globalChangesMade = false;

        for (int i = 0; i < cacheArr.length(); i++) {
            var localChangesMade = false;
            final JSONObject jsonObject = cacheArr.getJSONObject(i);

            for (Toggles toggle : Toggles.values()) {
                try {
                    JSONObject toggleObj = jsonObject.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString());

                    if (!toggleObj.has(toggle.toString())) {
                        globalChangesMade = true;
                        localChangesMade = true;
                        switch (toggle) {
                            case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> toggleObj.put(toggle.toString(), false);
                            default -> toggleObj.put(toggle.toString(), true);
                        }

                    }

                    if (!toggleObj.has(Toggles.TogglesConfigField.DJ_TOGGLES.toString())) {
                        globalChangesMade = true;
                        localChangesMade = true;
                        var djTogglesObj = new JSONObject();

                        for (ICommand musicCommand : new CommandManager(new EventWaiter()).getMusicCommands())
                            djTogglesObj.put(musicCommand.getName().toLowerCase(), false);

                        toggleObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
                    } else {
                        var djTogglesObj = toggleObj.getJSONObject(Toggles.TogglesConfigField.DJ_TOGGLES.toString());

                        for (ICommand musicCommand : new CommandManager(new EventWaiter()).getMusicCommands())
                            if (!djTogglesObj.has(musicCommand.getName())) {
                                globalChangesMade = true;
                                localChangesMade = true;
                                djTogglesObj.put(musicCommand.getName(), false);
                            }

                        toggleObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
                    }
                } catch (JSONException e) {
                    for (Toggles errToggles : Toggles.values())
                        switch (errToggles) {
                            case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> {
                                globalChangesMade = true;
                                localChangesMade = true;
                                jsonObject.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString())
                                        .put(errToggles.toString(), false);
                            }
                            default -> {
                                globalChangesMade = true;
                                localChangesMade = true;
                                jsonObject.getJSONObject(GuildsDB.Field.TOGGLES_OBJECT.toString())
                                        .put(errToggles.toString(), true);
                            }
                        }
                }
            }
            if (localChangesMade) objectsToUpdate.add(jsonObject);
        }
        if (globalChangesMade) getCache().updateCacheObjects(objectsToUpdate);
    }

    public JSONObject getDefaultToggleObject() {
        JSONObject toggleObj = new JSONObject();

        for (Toggles toggle : Toggles.values())
            try {
                if (!toggleObj.has(toggle.toString()))
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
                        case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> toggleObj.put(errToggles.toString(), false);
                        default -> toggleObj.put(errToggles.toString(), true);
                    }
            }
        return toggleObj;
    }

}
