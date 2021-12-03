package main.commands.commands.management.toggles.togglesconfig;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.constants.JSONConfigFile;
import main.utils.database.BotDB;
import main.utils.json.AbstractJSONConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class TogglesConfig extends AbstractJSONConfig {
    private final JSONConfigFile file = JSONConfigFile.TOGGLES;

    public TogglesConfig() {
        super(JSONConfigFile.TOGGLES);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile(file);
        } catch (IllegalStateException e) {
            updateFile();
            return;
        }

        var obj = new JSONObject();
        for (Guild g : new BotDB().getGuilds()) {
            var guildObj = new JSONObject();
            for (Toggles toggle : Toggles.values())
                guildObj.put(toggle.toString(), true);
            obj.put(g.getId(), guildObj);
        }

        setJSON(obj);
    }

    private void updateFile() {
        var obj = getJSONObject();

        for (Guild g : new BotDB().getGuilds())
            for (Toggles toggle : Toggles.values())
                try {
                    JSONObject guildObj = obj.getJSONObject(g.getId());
                    guildObj.put(toggle.toString(), true);

                    if (!guildObj.has(Toggles.TogglesConfigField.DJ_TOGGLES.toString())) {
                        var djTogglesObj = new JSONObject();

                        for (ICommand musicCommand : new CommandManager(new EventWaiter()).getMusicCommands())
                            djTogglesObj.put(musicCommand.getName().toLowerCase(), false);

                        guildObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
                    } else {
                        var djTogglesObj = guildObj.getJSONObject(Toggles.TogglesConfigField.DJ_TOGGLES.toString());

                        for (ICommand musicCommand : new CommandManager(new EventWaiter()).getMusicCommands())
                            if (!djTogglesObj.has(musicCommand.getName()))
                                djTogglesObj.put(musicCommand.getName(), false);

                        guildObj.put(Toggles.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj);
                    }
                } catch (JSONException e) {
                    obj.put(g.getId(), new JSONObject());
                    for (Toggles errToggles: Toggles.values())
                        obj.getJSONObject(g.getId()).put(errToggles.toString(), true);
                }

        setJSON(obj);
    }

    /**
     * Get the boolean value for the specific toggle
     * @param toggle The toggle to get the status for
     * @return The status of the toggle
     */
    public boolean getToggle(Guild guild, Toggles toggle) {
        var obj = getJSONObject();
        return obj.getJSONObject(guild.getId()).getBoolean(toggle.toString());
    }

    public HashMap<String, Boolean> getDJToggles(Guild g) {
        final HashMap<String, Boolean> ret = new HashMap<>();
        var obj = getJSONObject()
                .getJSONObject(g.getId())
                .getJSONObject(Toggles.TogglesConfigField.DJ_TOGGLES.toString());

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

    /**
     * Set the status of the specific toggle passed
     * @param toggle The toggle whose status is to be set
     * @param val The status to set the toggle to
     */
    public void setToggle(Guild guild, Toggles toggle, boolean val) {
        var obj = getJSONObject();
        obj.getJSONObject(guild.getId()).put(toggle.toString(), val);
        setJSON(obj);
    }

    public void setDJToggle(Guild guild, ICommand command, boolean val) {
        var obj = getJSONObject();
        var djObj = obj.getJSONObject(guild.getId())
                .getJSONObject(Toggles.TogglesConfigField.DJ_TOGGLES.toString());

        djObj.put(command.getName().toLowerCase(), val);

        setJSON(obj);
    }

    public boolean isDJToggleSet(Guild guild, ICommand cmd) {
        return getDJToggles(guild).containsKey(cmd.getName().toLowerCase());
    }

    public boolean isDJToggleSet(Guild guild, String cmd) {
        return getDJToggles(guild).containsKey(cmd.toLowerCase());
    }
}
