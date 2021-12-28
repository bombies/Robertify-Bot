package main.utils.json.legacy.restrictedchannels;

import com.google.common.collect.Lists;
import main.constants.JSONConfigFile;
import main.utils.database.sqlite3.BotDB;
import main.utils.json.legacy.AbstractJSONFile;
import main.utils.json.restrictedchannels.RestrictedChannelsConfigField;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class LegacyRestrictedChannelsConfig extends AbstractJSONFile {
    private final Logger logger = LoggerFactory.getLogger(LegacyRestrictedChannelsConfig.class);

    public LegacyRestrictedChannelsConfig() {
        super(JSONConfigFile.RESTRICTED_CHANNELS);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile();
        } catch (IllegalStateException e) {
            updateFile();
            return;
        }

        final var obj = new JSONObject();

        setJSON(obj);
    }

    private void updateFile() {
        final var obj = getJSONObject();

        for (Guild g : new BotDB().getGuilds())
            if (!obj.has(g.getId()))
                obj.put(g.getId(), new JSONArray());

            setJSON(obj);
    }

    public void addChannel(String gid, long channelID, ChannelType type) {
        if (isRestrictedChannel(gid, channelID, type))
            throw new IllegalStateException("This is already a restricted voice channel!");

        final var obj = getJSONObject();
        final RestrictedChannelsConfigField configField;

        switch (type) {
            case TEXT_CHANNEL -> configField = RestrictedChannelsConfigField.TEXT_CHANNELS;
            case VOICE_CHANNEL -> configField = RestrictedChannelsConfigField.VOICE_CHANNELS;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        if (!obj.has(gid)) {
            final var guildObj = new JSONObject();

            guildObj.put(RestrictedChannelsConfigField.TEXT_CHANNELS.toString(), new JSONArray());
            guildObj.put(RestrictedChannelsConfigField.VOICE_CHANNELS.toString(), new JSONArray());

            guildObj.getJSONArray(configField.toString()).put(channelID);

            obj.put(gid, guildObj);
        } else {
            obj.getJSONObject(gid).getJSONArray(configField.toString()).put(channelID);
        }

        setJSON(obj);
    }

    public void removeChannel(String gid, long channelID, ChannelType type) {
        final var obj = getJSONObject();

        if (!obj.has(gid))
            throw new NullPointerException("This guild couldn't be found with any channels!");

        final RestrictedChannelsConfigField configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = RestrictedChannelsConfigField.TEXT_CHANNELS;
            case VOICE_CHANNEL -> configField = RestrictedChannelsConfigField.VOICE_CHANNELS;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        final var guildObj = obj.getJSONObject(gid);

        try {
            final var arr = guildObj.getJSONArray(configField.toString());
            arr.remove(getIndexOfObjectInArray(arr, channelID));
            setJSON(obj);
        } catch (NullPointerException e) {
            throw new NullPointerException("This channel ID already isn't a restricted channel");
        }
    }

    public List<Long> getRestrictedChannels(String gid, ChannelType type) {
        final var obj = getJSONObject();

        if (!obj.has(gid))
            throw new NullPointerException("This guild could not be found with any channels!");

        final RestrictedChannelsConfigField configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = RestrictedChannelsConfigField.TEXT_CHANNELS;
            case VOICE_CHANNEL -> configField = RestrictedChannelsConfigField.VOICE_CHANNELS;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        try {
            final List<Long> ret = new ArrayList<>();
            final var arr = obj.getJSONObject(gid).getJSONArray(configField.toString());

            for (int i = 0; i < arr.length(); i++)
                ret.add(arr.getLong(i));

            return ret;
        } catch (JSONException e) {
            return Lists.newArrayList();
        }
    }

    public boolean isRestrictedChannel(String gid, long vcID, ChannelType type) {
        final var obj = getJSONObject();

        if (!obj.has(gid)) return false;

        return getRestrictedChannels(gid, type).contains(vcID);
    }

    public String restrictedChannelsToString(String gid, ChannelType type) {
        final var channels = getRestrictedChannels(gid, type);
        final var sb = new StringBuilder();

        for (var channelID : channels)
            sb.append("<#").append(channelID).append(">")
                    .append(channelID.equals(channels.get(channels.size()-1)) ? "" : ", ");

        return sb.toString();
    }

    public enum ChannelType {
        VOICE_CHANNEL("voice"),
        TEXT_CHANNEL("text"),
        ALL("all");

        private final String str;

        ChannelType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
