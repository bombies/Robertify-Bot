package main.utils.database.mongodb.cache.redis;

import com.mongodb.client.MongoCollection;
import main.constants.ENV;
import main.main.Config;
import main.utils.json.AbstractJSON;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.JedisPooled;

import java.util.HashMap;
import java.util.Map;

public abstract class RedisCache implements AbstractJSON {

    private final String cacheID;
    private final JedisPooled jedis;

    protected RedisCache(String cacheID) {
        this.cacheID = cacheID + "#"+ Config.get(ENV.MONGO_DATABASE_NAME) +"#";
        this.jedis = RedisDB.getInstance().getJedis();
    }

    void hsetJSON(String identifier, HashMap<String, JSONObject> hash) {
        final HashMap<String, String> newHash = new HashMap<>();
        hash.forEach((key, val) -> newHash.put(key, val.toString()));
        hset(identifier, newHash);
    }

    void hset(String identifier, HashMap<String, String> hash) {
        jedis.hset(cacheID + identifier, hash);
    }

    String hget(String identifier, String key) {
        return jedis.hget(cacheID + identifier, key);
    }

    JSONObject hgetJSON(String identifier, String key) {
        return new JSONObject(hget(identifier, key));
    }

    Map<String, String> hgetAll(String key) {
        return jedis.hgetAll(key);
    }

    void setex(String identifier, int seconds, String value) {
        jedis.setex(cacheID + identifier, seconds, value);
    }

    void setex(String identifier, int seconds, JSONObject value) {
        setex(identifier, seconds, value.toString());
    }

    void setex(long identifier, int seconds, JSONObject value) {
        setex(String.valueOf(identifier), seconds, value.toString());
    }

    String get(String identifier) {
        return jedis.get(cacheID + identifier);
    }

    String get(long identifier) {
        return get(String.valueOf(identifier));
    }

    void del(String identifier) {
        jedis.del(cacheID + identifier);
    }

    void del(long identifier) {
        del(String.valueOf(identifier));
    }

    public void updateCache(String identifier, Document document) {
        jedis.del(identifier);
        jedis.set(cacheID + identifier, document.toJson());
    }

    public void updateCache(String identifier, Document document, int expiration) {
        jedis.del(identifier);
        jedis.setex(cacheID + identifier, expiration, document.toJson());
    }

    public void updateCache(String identifier, JSONObject object) {
        jedis.del(identifier);
        jedis.set(cacheID + identifier, object.toString());
    }

    public void updateCache(String identifier, JSONObject object, int expiration) {
        jedis.del(identifier);
        jedis.setex(cacheID + identifier, expiration, object.toString());
    }

    public void updateCacheObjects(HashMap<String, JSONObject> objects) {
        final HashMap<String, Document> documents = new HashMap<>();
        objects.forEach((key, object) -> documents.put(key, Document.parse(object.toString())));
        updateCache(documents);
    }

    public void updateCache(HashMap<String, Document> documents) {

        for (var document : documents.entrySet()) {
            jedis.del(document.getKey());
            jedis.setex(cacheID + document.getKey(), 3600, document.getValue().toJson());
        }
    }

    public void removeFromCache(String id) {
        del(id);
    }

    public JSONObject getCacheJSON(String identifier) {
        return new JSONObject(get(identifier));
    }

    public JSONObject getJSON(String id) {
        String source = get(id);
        if (source == null)
            return null;
        return new JSONObject(source);
    }

    public JSONObject getJSONByGuild(String gid) {
        return getJSON(gid);
    }

    public JSONObject getJSONByGuild(long gid) {
        return getJSON(String.valueOf(gid));
    }

    private JSONObject collectionToJSON(MongoCollection<Document> collection) {
        JSONObject collectionObj = new JSONObject();
        JSONArray documentArr = new JSONArray();

        collection.find().forEach(doc -> documentArr.put(new JSONObject(doc.toJson())));

        collectionObj.put(DatabaseRedisCache.CacheField.DOCUMENTS.toString(), documentArr);
        return collectionObj;
    }

    public JSONObject getCache(String id) {
        return new JSONObject(get(cacheID + id));
    }

    boolean arrayHasObject(JSONArray array, ObjectId object) {
        for (int i = 0; i < array.length(); i++) {
            if (array.getJSONObject(i).getJSONObject("_id").getString("$oid").equals(object.toString()))
                return true;
        }
        return false;
    }

    int getIndexOfObjectInArray(JSONArray array, ObjectId object) {
        if (!arrayHasObject(array, object))
            throw new NullPointerException("There was no such object found in the array!");

        for (int i = 0; i < array.length(); i++)
            if (array.getJSONObject(i).getJSONObject("_id").getString("$oid").equals(object.toString()))
                return i;
        return -1;
    }
}
