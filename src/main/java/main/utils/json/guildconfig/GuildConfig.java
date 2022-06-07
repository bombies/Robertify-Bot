package main.utils.json.guildconfig;

import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuildConfig extends AbstractGuildConfig {
    private final static Logger logger = LoggerFactory.getLogger(GuildConfig.class);
    private final Guild guild;
    private final long gid;
    
    public GuildConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public void addGuild() {
        if (guildHasInfo())
            throw new IllegalArgumentException("This guild is already added!");

        getDatabase().addGuild(gid);
    }

    public void removeGuild() {
        getDatabase().removeGuild(gid);
        if (!guildHasInfo())
            logger.warn("There is no information for guild with ID {} in the cache.", gid);
        else
            unloadGuild();
    }

    public String getPrefix() {
        if (!guildHasInfo())
            loadGuild();

        return (String) getCache().getField(gid, GuildDB.Field.GUILD_PREFIX);
    }

    public void setPrefix(String prefix) {
        if (!guildHasInfo())
            loadGuild();

        if (prefix.length() > 4)
            throw new IllegalArgumentException("The prefix must be 4 or less characters!");

        getCache().setField(gid, GuildDB.Field.GUILD_PREFIX, prefix);
    }

    @Deprecated
    public long getAnnouncementChannelID() {
        if (!guildHasInfo())
            loadGuild();

        try {
            return (long) getCache().getField(gid, GuildDB.Field.ANNOUNCEMENT_CHANNEL);
        } catch (ClassCastException e) {
            return (int) getCache().getField(gid, GuildDB.Field.ANNOUNCEMENT_CHANNEL);
        }
    }

    @Deprecated
    public void setAnnouncementChannelID(long id) {
        if (!guildHasInfo())
            loadGuild();

        getCache().setField(gid, GuildDB.Field.ANNOUNCEMENT_CHANNEL, id);
    }

    @Deprecated
    public boolean announcementChannelIsSet() {
        if (!guildHasInfo())
            loadGuild();

        return getAnnouncementChannelID() != -1;
    }

    public List<BannedUser> getBannedUsers() {
        if (!guildHasInfo())
            loadGuild();

        final JSONArray bannedUsers = (JSONArray) getCache().getField(gid, GuildDB.Field.BANNED_USERS_ARRAY);
        final List<BannedUser> ret = new ArrayList<>();

        for (int i = 0; i < bannedUsers.length(); i++) {
            JSONObject jsonObject = bannedUsers.getJSONObject(i);
            BannedUser bannedUser = new BannedUser(
                    jsonObject.getLong(GuildDB.Field.BANNED_USER.toString()),
                    jsonObject.getLong(GuildDB.Field.BANNED_BY.toString()),
                    jsonObject.getLong(GuildDB.Field.BANNED_AT.toString()),
                    jsonObject.getLong(GuildDB.Field.BANNED_UNTIL.toString())
            );
            ret.add(bannedUser);
        }

        return ret;
    }

    public HashMap<Long, Long> getBannedUsersWithUnbanTimes() {
        if (!guildHasInfo())
            loadGuild();

        final var bannedUsers = getBannedUsers();
        final HashMap<Long, Long> ret = new HashMap<>();

        for (var bannedUser : bannedUsers)
            ret.put(bannedUser.user, bannedUser.bannedUntil);
        return ret;
    }

    public void banUser(long uid, long modId, long bannedAt, long bannedUntil) {
        if (!guildHasInfo())
            loadGuild();

        if (isBannedUser(uid))
            throw new IllegalArgumentException("This user is already banned!");

        final var guildObj = getCache().getGuildInfo(gid);
        final JSONArray bannedUsers = guildObj.getJSONArray(GuildDB.Field.BANNED_USERS_ARRAY.toString());

        bannedUsers.put(new JSONObject()
                .put(GuildDB.Field.BANNED_USER.toString(), uid)
                .put(GuildDB.Field.BANNED_BY.toString(), modId)
                .put(GuildDB.Field.BANNED_AT.toString(), bannedAt)
                .put(GuildDB.Field.BANNED_UNTIL.toString(), bannedUntil)
        );

        getCache().updateGuild(guildObj);
    }

    public void unbanUser(long uid) {
        if (!guildHasInfo())
            loadGuild();

        if (!isBannedUser(uid))
            throw new IllegalArgumentException("This user isn't banned!");

        final var guildObj = getCache().getGuildInfo(gid);
        final JSONArray bannedUsers = guildObj.getJSONArray(GuildDB.Field.BANNED_USERS_ARRAY.toString());
        bannedUsers.remove(getIndexOfObjectInArray(bannedUsers, GuildDB.Field.BANNED_USER, uid));

        getCache().updateGuild(guildObj);
    }

    public long getTimeUntilUnban(long uid) {
        if (!isBannedUser(uid))
            throw new IllegalArgumentException("This user isn't banned!");

        final var bannedUser = getBannedUsers().stream()
                .filter(user -> user.user == uid)
                .findFirst()
                .orElse(null);

        if (bannedUser == null)
            throw new NullPointerException("Now how did this happen?");

        return bannedUser.bannedUntil - bannedUser.bannedAt;
    }

    public boolean isBannedUser(long uid) {
        if (!guildHasInfo())
            loadGuild();

        for (var bannedUser : getBannedUsers())
            if (bannedUser.user == uid) return true;
        return false;
    }

    public boolean get247() {
        if (!guildHasInfo())
            loadGuild();

        if (!getCache().hasField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN)) {
            getCache().setField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN, false);
            return false;
        }

        return (boolean) getCache().getField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN);
    }

    public void set247(boolean status) {
        if (!guildHasInfo())
            loadGuild();

        getCache().setField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN, status);
    }

    @Override
    public void update() {
        // Nothing
    }

    public record BannedUser(long user, long bannedBy, long bannedAt, long bannedUntil) {
        @Override
        public String toString() {
            return String.valueOf(user);
        }
    }

}
