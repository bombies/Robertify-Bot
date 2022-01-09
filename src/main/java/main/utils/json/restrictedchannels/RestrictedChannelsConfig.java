package main.utils.json.restrictedchannels;

import main.utils.database.mongodb.GuildsDB;
import main.utils.json.AbstractGuildConfig;

import java.util.ArrayList;
import java.util.List;

public class RestrictedChannelsConfig extends AbstractGuildConfig {

    public void addChannel(long gid, long channelID, ChannelType type) {
        final GuildsDB.Field configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = GuildsDB.Field.RESTRICTED_CHANNELS_TEXT;
            case VOICE_CHANNEL -> configField = GuildsDB.Field.RESTRICTED_CHANNELS_VOICE;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        if (isRestrictedChannel(gid, channelID, type))
            throw new IllegalStateException("This is already a restricted voice channel!");

        final var obj = getGuildObject(gid);

        obj.getJSONObject(GuildsDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
                .getJSONArray(configField.toString()).put(channelID);

        getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
    }

    public void removeChannel(long gid, long channelID, ChannelType type) {
        final var obj = getGuildObject(gid);

        final GuildsDB.Field configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = GuildsDB.Field.RESTRICTED_CHANNELS_TEXT;
            case VOICE_CHANNEL -> configField = GuildsDB.Field.RESTRICTED_CHANNELS_VOICE;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        if (!isRestrictedChannel(gid, channelID, type))
            throw new IllegalStateException("This isn't a restricted channel!");

        try {
            final var arr = obj.getJSONObject(GuildsDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
                    .getJSONArray(configField.toString());
            arr.remove(getIndexOfObjectInArray(arr, channelID));
            getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
        } catch (NullPointerException e) {
            throw new NullPointerException("This channel ID already isn't a restricted channel");
        }
    }

    public List<Long> getRestrictedChannels(long gid, ChannelType type) {
        final var obj = getGuildObject(gid);

        final GuildsDB.Field configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = GuildsDB.Field.RESTRICTED_CHANNELS_TEXT;
            case VOICE_CHANNEL -> configField = GuildsDB.Field.RESTRICTED_CHANNELS_VOICE;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        final List<Long> ret = new ArrayList<>();
        final var arr = obj.getJSONObject(GuildsDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
                .getJSONArray(configField.toString());

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getLong(i));

        return ret;
    }

    public boolean isRestrictedChannel(long gid, long vcID, ChannelType type) {
        return getRestrictedChannels(gid, type).contains(vcID);
    }

    public String restrictedChannelsToString(long gid, ChannelType type) {
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

    @Override
    public void update() {

    }
}
