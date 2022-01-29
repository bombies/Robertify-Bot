package main.utils.json.guildconfig;

import main.utils.database.mongodb.databases.GuildsDB;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuildConfig extends AbstractGuildConfig {

    public void addGuild(long gid) {
        if (guildHasInfo(gid))
            throw new IllegalArgumentException("This guild is already added!");

        getCache().addToCache(GuildsDB.getGuildDocument(gid));
        getCache().updateCache();
    }

    public void removeGuild(long gid) {
        if (!guildHasInfo(gid))
            throw new IllegalArgumentException("There is already no information for this guild");

        getCache().removeFromCache(GuildsDB.Field.GUILD_ID, gid);
    }

    public String getPrefix(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        return (String) getCache().getField(gid, GuildsDB.Field.GUILD_PREFIX);
    }

    public void setPrefix(long gid, String prefix) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        if (prefix.length() > 4)
            throw new IllegalArgumentException("The prefix must be 4 or less characters!");

        getCache().setField(gid, GuildsDB.Field.GUILD_PREFIX, prefix);
    }

    public long getAnnouncementChannelID(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        try {
            return (long) getCache().getField(gid, GuildsDB.Field.ANNOUNCEMENT_CHANNEL);
        } catch (ClassCastException e) {
            return (int) getCache().getField(gid, GuildsDB.Field.ANNOUNCEMENT_CHANNEL);
        }
    }

    public void setAnnouncementChannelID(long gid, long id) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        getCache().setField(gid, GuildsDB.Field.ANNOUNCEMENT_CHANNEL, id);
    }

    public boolean announcementChannelIsSet(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        return getAnnouncementChannelID(gid) != -1;
    }

    public List<BannedUser> getBannedUsers(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        final JSONArray bannedUsers = (JSONArray) getCache().getField(gid, GuildsDB.Field.BANNED_USERS_ARRAY);
        final List<BannedUser> ret = new ArrayList<>();

        for (int i = 0; i < bannedUsers.length(); i++) {
            JSONObject jsonObject = bannedUsers.getJSONObject(i);
            BannedUser bannedUser = new BannedUser(
                    jsonObject.getLong(GuildsDB.Field.BANNED_USER.toString()),
                    jsonObject.getLong(GuildsDB.Field.BANNED_BY.toString()),
                    jsonObject.getLong(GuildsDB.Field.BANNED_AT.toString()),
                    jsonObject.getLong(GuildsDB.Field.BANNED_UNTIL.toString())
            );
            ret.add(bannedUser);
        }

        return ret;
    }

    public HashMap<Long, Long> getBannedUsersWithUnbanTimes(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        final var bannedUsers = getBannedUsers(gid);
        final HashMap<Long, Long> ret = new HashMap<>();

        for (var bannedUser : bannedUsers)
            ret.put(bannedUser.user, bannedUser.bannedUntil);
        return ret;
    }

    public void banUser(long gid, long uid, long modId, long bannedAt, long bannedUntil) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        if (isBannedUser(gid, uid))
            throw new IllegalArgumentException("This user is already banned!");

        final JSONArray bannedUsers = (JSONArray) getCache().getField(gid, GuildsDB.Field.BANNED_USERS_ARRAY);

        bannedUsers.put(new JSONObject()
                .put(GuildsDB.Field.BANNED_USER.toString(), uid)
                .put(GuildsDB.Field.BANNED_BY.toString(), modId)
                .put(GuildsDB.Field.BANNED_AT.toString(), bannedAt)
                .put(GuildsDB.Field.BANNED_UNTIL.toString(), bannedUntil)
        );

        getCache().setField(gid, GuildsDB.Field.BANNED_USERS_ARRAY, bannedUsers);
    }

    public void unbanUser(long gid, long uid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        if (!isBannedUser(gid, uid))
            throw new IllegalArgumentException("This user isn't banned!");

        final JSONArray bannedUsers = (JSONArray) getCache().getField(gid, GuildsDB.Field.BANNED_USERS_ARRAY);
        bannedUsers.remove(getIndexOfObjectInArray(bannedUsers, GuildsDB.Field.BANNED_USER, uid));

        getCache().setField(gid, GuildsDB.Field.BANNED_USERS_ARRAY, bannedUsers);
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
            throw new NullPointerException("This guild doesn't have any information!");

        for (var bannedUser : getBannedUsers(gid))
            if (bannedUser.user == uid) return true;
        return false;
    }

    public boolean get247(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        if (!getCache().hasField(gid, GuildsDB.Field.TWENTY_FOUR_SEVEN)) {
            getCache().setField(gid, GuildsDB.Field.TWENTY_FOUR_SEVEN, false);
            return false;
        }

        return (boolean) getCache().getField(gid, GuildsDB.Field.TWENTY_FOUR_SEVEN);
    }

    public void set247(long gid, boolean status) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        getCache().setField(gid, GuildsDB.Field.TWENTY_FOUR_SEVEN, status);
    }

    @Override
    public void update() {
        // Nothing
    }

    public static record BannedUser(long user, long bannedBy, long bannedAt, long bannedUntil) {
        @Override
        public String toString() {
            return String.valueOf(user);
        }
    }

}
