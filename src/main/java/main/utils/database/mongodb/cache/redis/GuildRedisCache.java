package main.utils.database.mongodb.cache.redis;

import lombok.Getter;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static main.utils.database.mongodb.databases.GuildDB.getGuildDocument;

public class GuildRedisCache extends AbstractRedisCache {
    private final static Logger logger = LoggerFactory.getLogger(GuildRedisCache.class);

    @Getter
    private static GuildRedisCache instance;

    protected GuildRedisCache() {
        super("ROBERTIFY_GUILD", GuildDB.ins());
    }

    public static void initCache() {
        logger.debug("Instantiating new Guild cache");
        instance = new GuildRedisCache();
        AbstractGuildConfig.initCache();
    }

    public synchronized Object getField(long gid, GuildDB.Field field) {
        if (!guildHasInfo(gid))
            loadGuild(gid);
        return getGuildInfo(gid).get(field.toString());
    }

    public synchronized void setField(long gid, GuildDB.Field field, Object value) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        JSONObject guildInfo = getGuildInfo(gid);
        guildInfo.put(field.toString(), value);
        setex(gid, 3600, guildInfo);
        updateGuild(guildInfo);
    }

    public synchronized void setFields(long gid, JSONObject fields) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        final var validKeys = Arrays.stream(GuildDB.Field.values())
                .map(GuildDB.Field::toString)
                .toList();
        final var guildInfo = getGuildInfo(gid);
        for (var key : fields.keySet()) {
            if (!validKeys.contains(key))
                continue;
            guildInfo.put(key, fields.get(key));
        }
        setex(gid, 3600, guildInfo);
        updateGuild(guildInfo);
    }

    public synchronized boolean hasField(long gid, GuildDB.Field field) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        return getGuildInfo(gid).has(field.toString());
    }

    public synchronized JSONObject getGuildInfo(long gid) {
        return getGuildInfo(String.valueOf(gid));
    }

    public synchronized JSONObject getGuildInfo(String gid) {
        if (!guildHasInfo(gid))
            loadGuild(gid);

        final String guildInfo = get(gid);
        if (guildInfo == null)
            return null;

        final var ret = new JSONObject(guildInfo);
        return correctGuildObj(ret);
    }

    public void updateGuild(JSONObject obj, long gid) {
        updateCache(correctGuildObj(obj), GuildDB.Field.GUILD_ID, gid);
    }

    public void updateGuild(JSONObject obj) {
        updateCache(correctGuildObj(obj), GuildDB.Field.GUILD_ID, GeneralUtils.getID(obj, GuildDB.Field.GUILD_ID));
    }

    public JSONObject correctGuildObj(JSONObject obj) {
        if (!obj.has(GuildDB.Field.GUILD_ID.toString()))
            return obj;

        if (obj.has("_id"))
            if (obj.get("_id") instanceof String)
                obj.put("_id", new ObjectId(obj.getString("_id")));

        if (obj.get(GuildDB.Field.GUILD_ID.toString()) instanceof String)
            obj.put(GuildDB.Field.GUILD_ID.toString(), Long.parseLong(obj.getString(GuildDB.Field.GUILD_ID.toString())));

        if (obj.has(GuildDB.Field.LOG_CHANNEL.toString()))
            if (obj.get(GuildDB.Field.LOG_CHANNEL.toString()) instanceof String)
                obj.put(GuildDB.Field.LOG_CHANNEL.toString(), Long.parseLong(obj.getString(GuildDB.Field.LOG_CHANNEL.toString())));

        if (obj.has(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())) {
            final var restrictedChannelObj = obj.getJSONObject(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString());
            var rtc = restrictedChannelObj.getJSONArray(GuildDB.Field.RESTRICTED_CHANNELS_TEXT.toString());
            var rvc = restrictedChannelObj.getJSONArray(GuildDB.Field.RESTRICTED_CHANNELS_VOICE.toString());

            if (!rtc.isEmpty()) {
                if (rtc.get(0) instanceof String) {
                    final var newArr = new JSONArray();
                    rtc.toList().forEach(item -> newArr.put(Long.parseLong((String) item)));
                    rtc = newArr;
                }
            }

            if (!rvc.isEmpty()) {
                if (rvc.get(0) instanceof String) {
                    final var newArr = new JSONArray();
                    rvc.toList().forEach(item -> newArr.put(Long.parseLong((String) item)));
                    rvc = newArr;
                }
            }

            obj.put(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString(), new JSONObject()
                    .put(GuildDB.Field.RESTRICTED_CHANNELS_TEXT.toString(), rtc)
                    .put(GuildDB.Field.RESTRICTED_CHANNELS_VOICE.toString(), rvc)
            );
        }

        final var permissionsObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString());
        for (final var code : Permission.getCodes()) {
            if (!permissionsObj.has(String.valueOf(code)))
                continue;

            var codeArr = permissionsObj.getJSONArray(String.valueOf(code));

            if (codeArr.isEmpty())
                continue;
            if (!(codeArr.get(0) instanceof String))
                continue;

            final var newArr = new JSONArray();
            codeArr.toList().forEach(item -> newArr.put(Long.parseLong((String)item)));
            permissionsObj.put(String.valueOf(code), newArr);
        }

        if (permissionsObj.has("users")) {
            final var usersObj = permissionsObj.getJSONObject("users");
            for (final var user : usersObj.keySet()) {
                final var userPermsArr = usersObj.getJSONArray(user);
                if (userPermsArr.isEmpty())
                    continue;
                if (!(userPermsArr.get(0) instanceof String))
                    continue;
                final var newArr = new JSONArray();
                userPermsArr.forEach(item -> newArr.put(Integer.parseInt((String)item)));
                usersObj.put(user, newArr);
            }
        }

        final var dedicatedChannelObj = obj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString());
        if (dedicatedChannelObj.get(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString()) instanceof String)
            dedicatedChannelObj.put(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), Long.parseLong(dedicatedChannelObj.getString(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString())));
        if (dedicatedChannelObj.get(GuildDB.Field.REQUEST_CHANNEL_ID.toString()) instanceof String)
            dedicatedChannelObj.put(GuildDB.Field.REQUEST_CHANNEL_ID.toString(), Long.parseLong(dedicatedChannelObj.getString(GuildDB.Field.REQUEST_CHANNEL_ID.toString())));

        if (obj.has(GuildDB.Field.BANNED_USERS_ARRAY.toString())) {
            final var bannedUserArr = obj.getJSONArray(GuildDB.Field.BANNED_USERS_ARRAY.toString());
            for (int i = 0; i < bannedUserArr.length(); i++) {
                final var bannedUserObj = bannedUserArr.getJSONObject(i);
                if (bannedUserObj.get(GuildDB.Field.BANNED_AT.toString()) instanceof String)
                    bannedUserObj.put(GuildDB.Field.BANNED_AT.toString(), Long.parseLong(bannedUserObj.getString(GuildDB.Field.BANNED_AT.toString())));
                if (bannedUserObj.get(GuildDB.Field.BANNED_USER.toString()) instanceof String)
                    bannedUserObj.put(GuildDB.Field.BANNED_USER.toString(), Long.parseLong(bannedUserObj.getString(GuildDB.Field.BANNED_USER.toString())));
                if (bannedUserObj.get(GuildDB.Field.BANNED_UNTIL.toString()) instanceof String)
                    bannedUserObj.put(GuildDB.Field.BANNED_UNTIL.toString(), Long.parseLong(bannedUserObj.getString(GuildDB.Field.BANNED_UNTIL.toString())));
                if (bannedUserObj.get(GuildDB.Field.BANNED_BY.toString()) instanceof String)
                    bannedUserObj.put(GuildDB.Field.BANNED_BY.toString(), Long.parseLong(bannedUserObj.getString(GuildDB.Field.BANNED_BY.toString())));
            }
        }
        return obj;
    }

    public synchronized boolean guildHasInfo(long gid) {
        return get(gid) != null;
    }

    public synchronized boolean guildHasInfo(String gid) {
        return get(gid) != null;
    }

    public synchronized void loadGuild(long gid) {
        loadGuild(String.valueOf(gid), 0);
    }

    public synchronized void loadGuild(String gid) {
        loadGuild(gid, 0);
    }

    /**
     * Recursively attempt to load the guild into cache
     * @param gid The ID of the guild
     * @param attempt The recursive attempt
     */
    private synchronized void loadGuild(String gid, int attempt) {
        logger.debug("Attempting to load guild with ID: {}", gid);
        try {
            String guildJSON = getDocument(GuildDB.Field.GUILD_ID.toString(), Long.parseLong(gid));

            if (guildJSON != null) {
                setex(gid, 3600, new JSONObject(guildJSON));
                logger.debug("Loaded guild with ID: {}", gid);
            }
        } catch (NullPointerException e) {
            if (attempt == 2)
                return;

            logger.debug("Guild with ID {} didn't exist in the database. Attempting to add and reload.", gid);
            addDocument(getGuildDocument(Long.parseLong(gid)));
            loadGuild(gid, ++attempt);
        }
    }

    public void unloadGuild(long gid) {
        del(gid);
    }

    public void loadAllGuilds() {
        logger.debug("Attempting to load all guilds");
        getCollection().find().forEach(document -> {
            JSONObject jsonObject = new JSONObject(document.toJson());
            final long gid = jsonObject.getLong(GuildDB.Field.GUILD_ID.toString());
            setex(gid, 3600, jsonObject);
            logger.debug("Loaded guild with id {}", gid);
        });
    }
}
