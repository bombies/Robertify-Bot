package main.utils.json.logs;

import main.main.Robertify;
import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bson.Document;
import org.json.JSONObject;

public class LogConfig extends AbstractGuildConfig {

    public long getChannelID(long gid) {
        if (!channelIsSet(gid))
            throw new NullPointerException("There is no channel for this guild! (ID="+gid+")");

        return getGuildObject(gid).getLong(Field.LOG_CHANNEL.name().toLowerCase());
    }

    public TextChannel getChannel(long gid) {
        return Robertify.shardManager.getTextChannelById(getChannelID(gid));
    }

    public boolean channelIsSet(long gid) {
        JSONObject guildObject = getGuildObject(gid);

        if (!guildObject.has(Field.LOG_CHANNEL.name().toLowerCase()))
            return false;

        return guildObject.getLong(Field.LOG_CHANNEL.name().toLowerCase()) != -1L;
    }

    public void setChannel(long gid, long cid) {
        JSONObject guildObject = getGuildObject(gid);
        guildObject.put(Field.LOG_CHANNEL.name().toLowerCase(), cid);

        getCache().updateGuild(guildObject, gid);
    }

    public void removeChannel(long gid) {
        JSONObject guildObject = getGuildObject(gid);
        guildObject.put(Field.LOG_CHANNEL.name().toLowerCase(), -1L);

        getCache().updateGuild(guildObject, gid);
    }

    @Override
    public void update(long gid) {
        JSONObject guildObject = getGuildObject(gid);

        if (!guildObject.has(Field.LOG_CHANNEL.name().toLowerCase()))
            guildObject.put(Field.LOG_CHANNEL.name().toLowerCase(), -1L);

        getCache().updateCache(Document.parse(guildObject.toString()));
    }

    public enum Field {
        LOG_CHANNEL
    }
}
