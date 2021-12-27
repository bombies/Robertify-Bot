package main.utils.json.guildconfig;

import lombok.Getter;
import main.utils.database.mongodb.GuildsDB;
import main.utils.database.mongodb.cache.GuildsDBCache;
import main.utils.json.AbstractJSON;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuildConfig implements AbstractJSON {
    @Getter
    private static final HashMap<Long, HashMap<Long, Long>> bannedUsers = new HashMap<>();
    private static final GuildsDBCache cache = GuildsDBCache.getInstance();

    public void addGuild(long gid) {
        if (guildHasInfo(gid))
            throw new IllegalArgumentException("This guild is already added!");

        cache.addToCache(GuildsDB.getGuildDocument(gid));
    }

    public void removeGuild(long gid) {
        if (!guildHasInfo(gid))
            throw new IllegalArgumentException("There is already no information for this guild");

        cache.removeFromCache(GuildsDB.Field.GUILD_ID, gid);
    }

    public String getPrefix(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        return (String) cache.getField(gid, GuildsDB.Field.GUILD_PREFIX);
    }

    public void setPrefix(long gid, String prefix) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        if (prefix.length() > 4)
            throw new IllegalArgumentException("The prefix must be 4 or less characters!");

        cache.setField(gid, GuildsDB.Field.GUILD_PREFIX, prefix);
    }

    public long getAnnouncementChannelID(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        try {
            return (long) cache.getField(gid, GuildsDB.Field.ANNOUNCEMENT_CHANNEL);
        } catch (ClassCastException e) {
            return (int) cache.getField(gid, GuildsDB.Field.ANNOUNCEMENT_CHANNEL);
        }
    }

    public void setAnnouncementChannelID(long gid, long id) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        cache.setField(gid, GuildsDB.Field.ANNOUNCEMENT_CHANNEL, id);
    }

    public List<BannedUser> getBannedUsers(long gid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        final JSONArray bannedUsers = (JSONArray) cache.getField(gid, GuildsDB.Field.BANNED_USERS_ARRAY);
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

        final JSONArray bannedUsers = (JSONArray) cache.getField(gid, GuildsDB.Field.BANNED_USERS_ARRAY);

        bannedUsers.put(new JSONObject()
                .put(GuildsDB.Field.BANNED_USER.toString(), uid)
                .put(GuildsDB.Field.BANNED_BY.toString(), modId)
                .put(GuildsDB.Field.BANNED_AT.toString(), bannedAt)
                .put(GuildsDB.Field.BANNED_UNTIL.toString(), bannedAt)
        );

        cache.setField(gid, GuildsDB.Field.BANNED_USERS_ARRAY, bannedUsers);
    }

    public void unbanUser(long gid, long uid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        if (!isBannedUser(gid, uid))
            throw new IllegalArgumentException("This user isn't banned!");

        final JSONArray bannedUsers = (JSONArray) cache.getField(gid, GuildsDB.Field.BANNED_USERS_ARRAY);
        bannedUsers.remove(getIndexOfObjectInArray(bannedUsers, GuildsDB.Field.BANNED_USER, uid));

        cache.setField(gid, GuildsDB.Field.BANNED_USERS_ARRAY, bannedUsers);
    }

    public boolean isBannedUser(long gid, long uid) {
        if (!guildHasInfo(gid))
            throw new NullPointerException("This guild doesn't have any information!");

        for (var bannedUser : getBannedUsers(gid))
            if (bannedUser.user == uid) return true;
        return false;
    }

    public boolean guildHasInfo(long gid) {
        return cache.guildHasInfo(gid);
    }

    public static record BannedUser(long user, long bannedBy, long bannedAt, long bannedUntil) {
        @Override
        public String toString() {
            return String.valueOf(user);
        }
    }

}