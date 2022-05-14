package main.utils.database.mongodb.cache.redis;

import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.json.AbstractJSON;
import org.json.JSONObject;
import redis.clients.jedis.JedisPooled;

import java.util.HashMap;
import java.util.Map;

public class AbstractRedisCache extends AbstractMongoDatabase implements AbstractJSON {

    private static AbstractRedisCache instance;
    private final String cacheID;
    private final AbstractMongoDatabase mongoDB;
    private final JedisPooled jedis;

    protected AbstractRedisCache(String cacheID, AbstractMongoDatabase mongoDB) {
        super(mongoDB);
        this.cacheID = cacheID + "#";
        this.jedis = RedisDB.getInstance().getJedis();
        this.mongoDB = mongoDB;
    }

    public void hsetJSON(String identifier, HashMap<String, JSONObject> hash) {
        final HashMap<String, String> newHash = new HashMap<>();
        hash.forEach((key, val) -> newHash.put(key, val.toString()));
        hset(identifier, newHash);
    }

    public void hset(String identifier, HashMap<String, String> hash) {
        jedis.hset(cacheID + identifier, hash);
    }

    public String hget(String identifier, String key) {
        return jedis.hget(cacheID + identifier, key);
    }

    public JSONObject hgetJSON(String identifier, String key) {
        return new JSONObject(hget(identifier, key));
    }

    public Map<String, String> hgetAll(String key) {
        return jedis.hgetAll(key);
    }

    public void setex(String identifier, int seconds, String value) {
        jedis.setex(cacheID + identifier, seconds, value);
    }

    public void setex(String identifier, int seconds, JSONObject value) {
        setex(identifier, seconds, value.toString());
    }

    public void setex(long identifier, int seconds, JSONObject value) {
        setex(String.valueOf(identifier), seconds, value.toString());
    }

    public String get(String identifier) {
        return jedis.get(cacheID + identifier);
    }

    public String get(long identifier) {
        return get(String.valueOf(identifier));
    }

    @Override
    public void init() {
        mongoDB.init();
    }
}
