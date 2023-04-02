package main.utils.json.autoplay;

import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

public class AutoPlayConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;

    public AutoPlayConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public boolean getStatus() {
        try {
            return getGuildObject().getBoolean(Field.AUTOPLAY.name().toLowerCase());
        } catch (JSONException e) {
            update();
            return getGuildObject().getBoolean(Field.AUTOPLAY.name().toLowerCase());
        }
    }

    public void setStatus(boolean status) {
        JSONObject guildObject = getGuildObject();
        guildObject.put(Field.AUTOPLAY.name().toLowerCase(), status);
        getCache().updateGuild(guildObject, gid);
    }

    @Override
    public void update() {
        JSONObject guildObject = getGuildObject();

        if (!guildObject.has(Field.AUTOPLAY.name().toLowerCase()))
            guildObject.put(Field.AUTOPLAY.name().toLowerCase(), false);
        getCache().updateCache(String.valueOf(gid), Document.parse(guildObject.toString()));
    }

    public enum Field {
        AUTOPLAY
    }
}
