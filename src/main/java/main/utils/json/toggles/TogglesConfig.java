package main.utils.json.toggles;

import main.commands.prefixcommands.CommandManager;
import main.commands.prefixcommands.ICommand;
import main.commands.slashcommands.SlashCommandManager;
import main.constants.Toggles;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.GuildDBCache;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import main.utils.json.logs.LogType;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class TogglesConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;

    public TogglesConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public boolean getToggle(Toggles toggle) {
        if (!guildHasInfo())
            loadGuild();

        try {
            return getTogglesObject().getBoolean(toggle.toString());
        } catch (JSONException e) {
            if (e.getMessage().contains("is not a")) {
                return getTogglesObject().getBoolean(toggle.toString());
            } else {
                JSONObject togglesObject = getTogglesObject();
                togglesObject.put(toggle.toString(), true);
                getCache().setField(guild.getIdLong(), GuildDB.Field.TOGGLES_OBJECT, togglesObject);
                return true;
            }
        }
    }

    public HashMap<String, Boolean> getDJToggles() {
        final HashMap<String, Boolean> ret = new HashMap<>();
        final var obj = getTogglesObject()
                .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString());

        for (String key : obj.keySet())
            ret.put(key, false);

        ret.replaceAll((k, v) -> obj.getBoolean(k));

        return ret;
    }

    public HashMap<String, Boolean> getLogToggles() {
        final HashMap<String, Boolean> ret = new HashMap<>();
        final var obj = getTogglesObject()
                .getJSONObject(Toggles.TogglesConfigField.LOG_TOGGLES.toString());

        for (final var key : obj.keySet())
            ret.put(key, true);

        ret.replaceAll((k,v) -> obj.getBoolean(k));

        return ret;
    }

    public boolean getDJToggle(AbstractSlashCommand cmd) {
        final var djToggles = getDJToggles();

        if (!djToggles.containsKey(cmd.getName())) {
            if (SlashCommandManager.getInstance().isMusicCommand(cmd)) {
                setDJToggle(cmd, false);
                return false;
            } else {
                throw new NullPointerException("Invalid command passed! [Command: "+cmd.getName()+"]");
            }
        }

        return djToggles.get(cmd.getName().toLowerCase());
    }

    @Deprecated
    public boolean getDJToggle(ICommand cmd) {
        final var djToggles = getDJToggles();

        if (!djToggles.containsKey(cmd.getName())) {
            if (new CommandManager().isMusicCommand(cmd)) {
                setDJToggle(cmd, false);
                return false;
            } else {
                throw new NullPointerException("Invalid command passed! [Command: "+cmd.getName()+"]");
            }
        }

        return djToggles.get(cmd.getName().toLowerCase());
    }

    public void setToggle(Toggles toggle, boolean val) {
        final var obj = getGuildObject();
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString()).put(toggle.toString(), val);
        getCache().updateGuild(obj, guild.getIdLong());
    }

    public void setDJToggle(AbstractSlashCommand command, boolean val) {
        final var obj = getGuildObject();
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString())
                .put(command.getName(), val);
        getCache().updateGuild(obj, guild.getIdLong());
    }

    @Deprecated
    public void setDJToggle(ICommand command, boolean val) {
        final var obj = getGuildObject();
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString())
                .put(command.getName(), val);
        getCache().updateGuild(obj, guild.getIdLong());
    }

    public void setLogToggle(LogType type, boolean state) {
        if (!isLogToggleSet(type))
            update();

        final var obj = getGuildObject();
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                .getJSONObject(GuildDB.Field.TOGGLES_LOGS.toString())
                .put(type.name().toLowerCase(), state);
        getCache().updateGuild(obj, guild.getIdLong());
    }

    public boolean getLogToggle(LogType type) {
        if (!isLogToggleSet(type))
            update();

        final var obj = getGuildObject();
        return obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                .getJSONObject(GuildDB.Field.TOGGLES_LOGS.toString())
                .getBoolean(type.name().toLowerCase());
    }

    public boolean isDJToggleSet(AbstractSlashCommand cmd) {
        return getDJToggles().containsKey(cmd.getName().toLowerCase());
    }

    @Deprecated
    public boolean isDJToggleSet(ICommand cmd) {
        return getDJToggles().containsKey(cmd.getName().toLowerCase());
    }

    public boolean isDJToggleSet(String cmd) {
        return getDJToggles().containsKey(cmd.toLowerCase());
    }

    public boolean isLogToggleSet(LogType type) {
        try {
            return getLogToggles().containsKey(type.name().toLowerCase());
        } catch (JSONException e) {
            return false;
        }
    }

    private JSONObject getTogglesObject() {
        return getGuildObject().getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString());
    }

    @Override
    public void update() {
        if (!guildHasInfo())
            loadGuild();

        final JSONArray cacheArr = GuildDBCache.getInstance().getCache();
        JSONObject object = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, gid));

        for (Toggles toggle : Toggles.values()) {
            try {
                JSONObject toggleObj = object.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString());
                getTogglesObject(toggleObj, toggle);
            } catch (JSONException e) {
                for (Toggles errToggles : Toggles.values())
                    switch (errToggles) {
                        case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> object.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                                .put(errToggles.toString(), false);
                        default -> object.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                                .put(errToggles.toString(), true);
                    }
            }
        }

        getCache().updateCache(String.valueOf(gid), Document.parse(object.toString()));
    }

    public static JSONObject getDefaultToggleObject() {
        JSONObject toggleObj = new JSONObject();

        for (Toggles toggle : Toggles.values())
            try {
                getTogglesObject(toggleObj, toggle);
            } catch (JSONException e) {
                for (Toggles errToggles : Toggles.values())
                    switch (errToggles) {
                        case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> toggleObj.put(errToggles.toString(), false);
                        default -> toggleObj.put(errToggles.toString(), true);
                    }
            }
        return toggleObj;
    }

    private static void getTogglesObject(JSONObject toggleObj, Toggles toggle) {
        if (!toggleObj.has(toggle.toString()))
            switch (toggle) {
                case RESTRICTED_VOICE_CHANNELS, RESTRICTED_TEXT_CHANNELS -> toggleObj.put(toggle.toString(), false);
                default -> toggleObj.put(toggle.toString(), true);
            }

        if (!toggleObj.has(Toggles.TogglesConfigField.DJ_TOGGLES.toString())) {
            var djTogglesObj = new JSONObject();

            for (AbstractSlashCommand musicCommand : SlashCommandManager.getInstance().getMusicCommands())
                djTogglesObj.put(musicCommand.getName().toLowerCase(), false);

            toggleObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
        } else {
            var djTogglesObj = toggleObj.getJSONObject(Toggles.TogglesConfigField.DJ_TOGGLES.toString());

            for (ICommand musicCommand : new CommandManager().getMusicCommands())
                if (!djTogglesObj.has(musicCommand.getName()))
                    djTogglesObj.put(musicCommand.getName(), false);

            toggleObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
        }

        if (!toggleObj.has(Toggles.TogglesConfigField.LOG_TOGGLES.toString())) {
            final var logObj = new JSONObject();

            for (final var type : LogType.values())
                logObj.put(type.name().toLowerCase(), true);

            toggleObj.put(Toggles.TogglesConfigField.LOG_TOGGLES.toString(), logObj);
        } else {
            final var logObj = toggleObj.getJSONObject(Toggles.TogglesConfigField.LOG_TOGGLES.toString());

            for (final var type : LogType.values())
                if (!logObj.has(type.name().toLowerCase()))
                    logObj.put(type.name().toLowerCase(), true);

            toggleObj.put(Toggles.TogglesConfigField.LOG_TOGGLES.toString(), logObj);
        }
    }

}
