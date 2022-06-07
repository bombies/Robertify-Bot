package main.utils.json.restrictedchannels;

import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.List;

public class RestrictedChannelsConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;

    public RestrictedChannelsConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public void addChannel(long channelID, ChannelType type) {
        final GuildDB.Field configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = GuildDB.Field.RESTRICTED_CHANNELS_TEXT;
            case VOICE_CHANNEL -> configField = GuildDB.Field.RESTRICTED_CHANNELS_VOICE;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        if (isRestrictedChannel(channelID, type))
            throw new IllegalStateException("This is already a restricted voice channel!");

        final var obj = getGuildObject();

        obj.getJSONObject(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
                .getJSONArray(configField.toString()).put(channelID);

        getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
    }

    public void removeChannel(long channelID, ChannelType type) {
        final var obj = getGuildObject();

        final GuildDB.Field configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = GuildDB.Field.RESTRICTED_CHANNELS_TEXT;
            case VOICE_CHANNEL -> configField = GuildDB.Field.RESTRICTED_CHANNELS_VOICE;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        if (!isRestrictedChannel(channelID, type))
            throw new IllegalStateException("This isn't a restricted channel!");

        try {
            final var arr = obj.getJSONObject(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
                    .getJSONArray(configField.toString());
            arr.remove(getIndexOfObjectInArray(arr, channelID));
            getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
        } catch (NullPointerException e) {
            throw new NullPointerException("This channel ID already isn't a restricted channel");
        }
    }

    public List<Long> getRestrictedChannels(ChannelType type) {
        final var obj = getGuildObject();

        final GuildDB.Field configField;
        switch (type) {
            case TEXT_CHANNEL -> configField = GuildDB.Field.RESTRICTED_CHANNELS_TEXT;
            case VOICE_CHANNEL -> configField = GuildDB.Field.RESTRICTED_CHANNELS_VOICE;
            default -> throw new IllegalArgumentException("Invalid type!");
        }

        final List<Long> ret = new ArrayList<>();
        final var arr = obj.getJSONObject(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
                .getJSONArray(configField.toString());

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getLong(i));

        return ret;
    }

    public boolean isRestrictedChannel(long vcID, ChannelType type) {
        return getRestrictedChannels(type).contains(vcID);
    }

    public String restrictedChannelsToString(ChannelType type) {
        final var channels = getRestrictedChannels(type);
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
