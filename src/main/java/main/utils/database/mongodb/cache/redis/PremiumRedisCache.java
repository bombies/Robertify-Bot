package main.utils.database.mongodb.cache.redis;

import lombok.Getter;
import main.utils.database.mongodb.databases.PremiumDB;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PremiumRedisCache extends AbstractRedisCache {
    private final static Logger logger = LoggerFactory.getLogger(PremiumRedisCache.class);
    @Getter
    private static PremiumRedisCache instance;

    protected PremiumRedisCache() {
        super("ROBERTIFY_PREMIUM", PremiumDB.ins());
    }

    public static void initCache() {
        logger.debug("Instantiating new premium cache");
        instance = new PremiumRedisCache();
    }

    public void addUser(String id, int premiumType, int premiumTier, String premiumStarted, String premiumExpires) {
        addToCache(id, new JSONObject()
                .put(PremiumDB.Field.USER_ID.toString(), id)
                .put(PremiumDB.Field.PREMIUM_TYPE.toString(), premiumType)
                .put(PremiumDB.Field.PREMIUM_TIER.toString(), premiumTier)
                .put(PremiumDB.Field.PREMIUM_SERVERS.toString(), new JSONArray())
                .put(PremiumDB.Field.PREMIUM_STARTED.toString(), premiumStarted)
                .put(PremiumDB.Field.PREMIUM_EXPIRES.toString(), premiumExpires)
        );
    }

    public PremiumUser getUser(String id) {
        try {
            final var userObj = getCache(id);
            if (userObj == null)
                return null;
            return new PremiumUser(
                    userObj.getString(PremiumDB.Field.USER_ID.toString()),
                    userObj.getString(PremiumDB.Field.PREMIUM_STARTED.toString()),
                    userObj.getString(PremiumDB.Field.PREMIUM_EXPIRES.toString()),
                    userObj.getInt(PremiumDB.Field.PREMIUM_TYPE.toString()),
                    userObj.getInt(PremiumDB.Field.PREMIUM_TIER.toString()),
                    userObj.getJSONArray(PremiumDB.Field.PREMIUM_SERVERS.toString()).toList().stream().map(String::valueOf).toList()
            );
        } catch (Exception e) {
            return null;
        }
    }

    public static class PremiumUser {
        @Getter
        private final String id, premiumStarted, premiumExpires;
        @Getter
        private final int premiumType, premiumTier;
        @Getter
        private final List<String> premiumServers;

        public PremiumUser(String id, String premiumStarted, String premiumExpires, int premiumType, int premiumTier, List<String> premiumServers) {
            this.id = id;
            this.premiumStarted = premiumStarted;
            this.premiumExpires = premiumExpires;
            this.premiumType = premiumType;
            this.premiumTier = premiumTier;
            this.premiumServers = premiumServers;
        }
    }
}
