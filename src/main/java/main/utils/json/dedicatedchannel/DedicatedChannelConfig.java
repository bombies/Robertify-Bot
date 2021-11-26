package main.utils.json.dedicatedchannel;

import main.constants.JSONConfigFile;
import main.main.Robertify;
import main.utils.database.BotUtils;
import main.utils.json.JSONConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONException;
import org.json.JSONObject;

public class DedicatedChannelConfig extends JSONConfig {
    public DedicatedChannelConfig() {
        super(JSONConfigFile.DEDICATED_CHANNELS);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile(JSONConfigFile.DEDICATED_CHANNELS);
        } catch (IllegalStateException e) {
            updateConfig();
            return;
        }

        final var obj = new JSONObject();
        for (Guild g : new BotUtils().getGuilds())
            obj.put(g.getId(), "");
        setJSON(obj);
    }

    private synchronized void updateConfig() {
        var obj = getJSONObject();

        for (Guild g : new BotUtils().getGuilds())
            try {
                obj.getString(g.getId());
            } catch (JSONException e) {
                obj.put(g.getId(), "");
            }
    }

    public synchronized DedicatedChannelConfig setChannel(String gid, String cid) {
        var obj = getJSONObject();

        var guild = new JSONObject();
        guild.put(DedicatedChannelConfigField.CHANNEL_ID.toString(), cid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized DedicatedChannelConfig setMessage(String gid, String mid) {
        var obj = getJSONObject();

        var guild = new JSONObject();
        guild.put(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString(), mid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized DedicatedChannelConfig setChannelAndMessage(String gid, String cid, String mid) {
        var obj = getJSONObject();

        var guild = new JSONObject();
        guild.put(DedicatedChannelConfigField.CHANNEL_ID.toString(), cid);
        guild.put(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString(), mid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized DedicatedChannelConfig removeChannel(String gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");

        var obj = getJSONObject();
        obj.remove(gid);

        setJSON(obj);
        return this;
    }

    public synchronized boolean isChannelSet(String gid) {
        try {
            getJSONObject().getJSONObject(gid);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public synchronized String getChannelID(String gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getJSONObject().getJSONObject(gid).getString(DedicatedChannelConfigField.CHANNEL_ID.toString());
    }

    public synchronized String getMessageID(String gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getJSONObject().getJSONObject(gid).getString(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString());
    }

    public synchronized TextChannel getTextChannel(String gid) {
        return Robertify.api.getTextChannelById(getChannelID(gid));
    }

    public synchronized Message getMessage(String gid) {
        return getTextChannel(gid).getHistory().getMessageById(getMessageID(gid));
    }
}
