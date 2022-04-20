package main.utils.json.guildconfig;

import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuildConfig extends AbstractGuildConfig {
    private final static Logger logger = LoggerFactory.getLogger(GuildConfig.class);

    public void addGuild(long gid) {
        if (guildHasInfo(gid))
            throw new IllegalArgumentException("This guild is already added!");

        getDatabase().addGuild(gid);
    }

    public void removeGuild(long gid) {
        getDatabase().removeGuild(gid);
        if (!guildHasInfo(gid))
            logger.warn("There is no information for guild with ID {} in the cache.", gid);
        else
            unloadGuild(gid);
    }

    public String getPrefix(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        return (String) getCache().getField(gid, GuildDB.Field.GUILD_PREFIX);
    }

    public void setPrefix(long gid, String prefix) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        if (prefix.length() > 4)
            throw new IllegalArgumentException("The prefix must be 4 or less characters!");

        getCache().setField(gid, GuildDB.Field.GUILD_PREFIX, prefix);
    }

    @Deprecated
    public long getAnnouncementChannelID(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        try {
            return (long) getCache().getField(gid, GuildDB.Field.ANNOUNCEMENT_CHANNEL);
        } catch (ClassCastException e) {
            return (int) getCache().getField(gid, GuildDB.Field.ANNOUNCEMENT_CHANNEL);
        }
    }

    @Deprecated
    public void setAnnouncementChannelID(long gid, long id) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        getCache().setField(gid, GuildDB.Field.ANNOUNCEMENT_CHANNEL, id);
    }

    @Deprecated
    public boolean announcementChannelIsSet(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        return getAnnouncementChannelID(gid) != -1;
    }

    public List<BannedUser> getBannedUsers(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

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

    public HashMap<Long, Long> getBannedUsersWithUnbanTimes(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        final var bannedUsers = getBannedUsers(gid);
        final HashMap<Long, Long> ret = new HashMap<>();

        for (var bannedUser : bannedUsers)
            ret.put(bannedUser.user, bannedUser.bannedUntil);
        return ret;
    }

    public void banUser(long gid, long uid, long modId, long bannedAt, long bannedUntil) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        if (isBannedUser(gid, uid))
            throw new IllegalArgumentException("This user is already banned!");

        final JSONArray bannedUsers = (JSONArray) getCache().getField(gid, GuildDB.Field.BANNED_USERS_ARRAY);

        bannedUsers.put(new JSONObject()
                .put(GuildDB.Field.BANNED_USER.toString(), uid)
                .put(GuildDB.Field.BANNED_BY.toString(), modId)
                .put(GuildDB.Field.BANNED_AT.toString(), bannedAt)
                .put(GuildDB.Field.BANNED_UNTIL.toString(), bannedUntil)
        );

        getCache().setField(gid, GuildDB.Field.BANNED_USERS_ARRAY, bannedUsers);
    }

    public void unbanUser(long gid, long uid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        if (!isBannedUser(gid, uid))
            throw new IllegalArgumentException("This user isn't banned!");

        final JSONArray bannedUsers = (JSONArray) getCache().getField(gid, GuildDB.Field.BANNED_USERS_ARRAY);
        bannedUsers.remove(getIndexOfObjectInArray(bannedUsers, GuildDB.Field.BANNED_USER, uid));

        getCache().setField(gid, GuildDB.Field.BANNED_USERS_ARRAY, bannedUsers);
    }

    public long getTimeUntilUnban(long gid, long uid) {
        if (!isBannedUser(gid, uid))
            throw new IllegalArgumentException("This user isn't banned!");

        final var bannedUser = getBannedUsers(gid).stream()
                .filter(user -> user.user == uid)
                .findFirst()
                .orElse(null);

        if (bannedUser == null)
            throw new NullPointerException("Now how did this happen?");

        return bannedUser.bannedUntil - bannedUser.bannedAt;
    }

    public boolean isBannedUser(long gid, long uid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        for (var bannedUser : getBannedUsers(gid))
            if (bannedUser.user == uid) return true;
        return false;
    }

    public boolean get247(long gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        if (!getCache().hasField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN)) {
            getCache().setField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN, false);
            return false;
        }

        return (boolean) getCache().getField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN);
    }

    public void set247(long gid, boolean status) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        getCache().setField(gid, GuildDB.Field.TWENTY_FOUR_SEVEN, status);
    }

    @Override
    public void update(long gid) {
        // Nothing
    }

    public record BannedUser(long user, long bannedBy, long bannedAt, long bannedUntil) {
        @Override
        public String toString() {
            return String.valueOf(user);
        }
    }

}
