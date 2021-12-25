package main.utils.json;

import main.constants.JSONConfigFile;
import main.utils.database.sqlite3.BotDB;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EightBallConfig extends AbstractJSONConfig {

    public EightBallConfig() {
        super(JSONConfigFile.EIGHT_BALL);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile();
        } catch (IllegalStateException e) {
            updateConfig();
            return;
        }

        final var jsonObject = new JSONObject();

        for (Guild guild : new BotDB().getGuilds())
            jsonObject.put(guild.getId(), new JSONArray());

        setJSON(jsonObject);
    }

    private synchronized void updateConfig() {
        var obj = getJSONObject();

        for (Guild g : new BotDB().getGuilds())
            try {
                obj.getJSONArray(g.getId());
            } catch (JSONException e) {
                obj.put(g.getId(), new JSONArray());
            }

        setJSON(obj);
    }

    public void addGuild(String gid) {
        var obj = getJSONObject();
        obj.put(gid, new JSONArray());
        setJSON(obj);
    }

    public EightBallConfig addResponse(String gid, String response) {
        var obj = getJSONObject();
        obj.getJSONArray(gid).put(response);
        setJSON(obj);
        return this;
    }

    public EightBallConfig removeResponse(String gid, int responseIndex) {
        var obj = getJSONObject();
        obj.getJSONArray(gid).remove(responseIndex);
        setJSON(obj);
        return this;
    }

    public EightBallConfig removeAllResponses(String gid) {
        var obj = getJSONObject();
        obj.getJSONArray(gid).clear();
        setJSON(obj);
        return this;
    }

    public List<String> getResponses(String gid) {
        final var obj = getJSONObject().getJSONArray(gid);
        final List<String> responses = new ArrayList<>();

        for (int i = 0; i < obj.length(); i++)
            responses.add(obj.getString(i));

        return responses;
    }
}
