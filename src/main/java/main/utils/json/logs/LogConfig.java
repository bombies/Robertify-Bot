package main.utils.json.logs;

import main.main.Robertify;
import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bson.Document;
import org.json.JSONObject;

public class LogConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;
    
    public LogConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public long getChannelID() {
        if (!channelIsSet())
            throw new NullPointerException("There is no channel for this guild! (ID="+gid+")");

        return getGuildObject().getLong(Field.LOG_CHANNEL.name().toLowerCase());
    }

    public TextChannel getChannel() {
        return Robertify.shardManager.getTextChannelById(getChannelID());
    }

    public boolean channelIsSet() {
        JSONObject guildObject = getGuildObject();

        if (!guildObject.has(Field.LOG_CHANNEL.name().toLowerCase()))
            return false;

        if (guildObject.get(Field.LOG_CHANNEL.name().toLowerCase()) == null)
            return false;

        return guildObject.getLong(Field.LOG_CHANNEL.name().toLowerCase()) != -1L;
    }

    public void setChannel(long cid) {
        JSONObject guildObject = getGuildObject();
        guildObject.put(Field.LOG_CHANNEL.name().toLowerCase(), cid);

        getCache().updateGuild(guildObject, gid);
    }

    public void removeChannel() {
        JSONObject guildObject = getGuildObject();
        guildObject.put(Field.LOG_CHANNEL.name().toLowerCase(), -1L);

        getCache().updateGuild(guildObject, gid);
    }

    @Override
    public void update() {
        JSONObject guildObject = getGuildObject();

        if (!guildObject.has(Field.LOG_CHANNEL.name().toLowerCase()))
            guildObject.put(Field.LOG_CHANNEL.name().toLowerCase(), -1L);

        getCache().updateCache(String.valueOf(gid), Document.parse(guildObject.toString()));
    }

    public enum Field {
        LOG_CHANNEL
    }
}
