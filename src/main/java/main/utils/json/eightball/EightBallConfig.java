package main.utils.json.eightball;

import main.utils.database.mongodb.databases.GuildsDB;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class EightBallConfig extends AbstractGuildConfig {

    public void addResponse(long gid, String response) {
        var obj = getGuildObject(gid);
        JSONArray array = obj.getJSONArray(GuildsDB.Field.EIGHT_BALL_ARRAY.toString())
                .put(response);
        getCache().setField(gid, GuildsDB.Field.EIGHT_BALL_ARRAY, array);
    }

    public void removeResponse(long gid, int responseIndex) {
        var obj = getGuildObject(gid);
        JSONArray array = obj.getJSONArray(GuildsDB.Field.EIGHT_BALL_ARRAY.toString());
        array.remove(responseIndex);
        getCache().setField(gid, GuildsDB.Field.EIGHT_BALL_ARRAY, array);
    }

    public void removeAllResponses(long gid) {
        var obj = getGuildObject(gid);
        JSONArray array = obj.getJSONArray(GuildsDB.Field.EIGHT_BALL_ARRAY.toString());
        array.clear();

        getCache().setField(gid, GuildsDB.Field.EIGHT_BALL_ARRAY, array);
    }

    public List<String> getResponses(long gid) {
        final var obj = getGuildObject(gid)
                .getJSONArray(GuildsDB.Field.EIGHT_BALL_ARRAY.toString());
        final List<String> responses = new ArrayList<>();

        for (int i = 0; i < obj.length(); i++)
            responses.add(obj.getString(i));

        return responses;
    }

    @Override
    public void update() {
        // Nothing
    }
}
