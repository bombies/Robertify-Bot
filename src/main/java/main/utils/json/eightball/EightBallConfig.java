package main.utils.json.eightball;

import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class EightBallConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;

    public EightBallConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public void addResponse(String response) {
        var obj = getGuildObject();
        JSONArray array = obj.getJSONArray(GuildDB.Field.EIGHT_BALL_ARRAY.toString())
                .put(response);
        getCache().setField(gid, GuildDB.Field.EIGHT_BALL_ARRAY, array);
    }

    public void removeResponse(int responseIndex) {
        var obj = getGuildObject();
        JSONArray array = obj.getJSONArray(GuildDB.Field.EIGHT_BALL_ARRAY.toString());
        array.remove(responseIndex);
        getCache().setField(gid, GuildDB.Field.EIGHT_BALL_ARRAY, array);
    }

    public void removeAllResponses() {
        var obj = getGuildObject();
        JSONArray array = obj.getJSONArray(GuildDB.Field.EIGHT_BALL_ARRAY.toString());
        array.clear();

        getCache().setField(gid, GuildDB.Field.EIGHT_BALL_ARRAY, array);
    }

    public List<String> getResponses() {
        final var obj = getGuildObject()
                .getJSONArray(GuildDB.Field.EIGHT_BALL_ARRAY.toString());
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
