package main.commands.commands.management.toggles.togglesconfig;

import main.constants.JSONConfigFile;
import main.utils.database.BotUtils;
import main.utils.json.JSONConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONException;
import org.json.JSONObject;

public class TogglesConfig extends JSONConfig {
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
        for (Guild g : new BotUtils().getGuilds()) {
            var guildObj = new JSONObject();
            for (Toggles toggle : Toggles.values())
                guildObj.put(toggle.toString(), true);
            obj.put(g.getId(), guildObj);
        }

        setJSON(obj);
    }

    private void updateFile() {
        var obj = getJSONObject();

        for (Guild g : new BotUtils().getGuilds())
            for (Toggles toggle : Toggles.values())
                try {
                    obj.getJSONObject(g.getId()).put(toggle.toString(), true);
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
}
