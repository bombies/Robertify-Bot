package main.utils.database.mongodb.cache.redis;

import com.mongodb.client.MongoCollection;
import lombok.Getter;
import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractJSON;
import main.utils.json.GenericJSONField;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.JedisPooled;

import java.util.HashMap;
import java.util.Map;

public class AbstractRedisCache extends AbstractMongoDatabase implements AbstractJSON {

    private static AbstractRedisCache instance;
    private final String cacheID;

    @Getter
    private final AbstractMongoDatabase mongoDB;
    @Getter
    private MongoCollection<Document> collection;
    private final JedisPooled jedis;

    protected AbstractRedisCache(String cacheID, AbstractMongoDatabase mongoDB) {
        super(mongoDB);
        this.collection = mongoDB.getCollection();
        this.cacheID = cacheID + "#" + Config.get(ENV.MONGO_DATABASE_NAME) + "#";
        this.jedis = RedisDB.getInstance().getJedis();
        this.mongoDB = mongoDB;
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

    @Override
    public void init() {
        mongoDB.init();
    }

    public void addToCache(String identifier, JSONObject obj) {
        final var objectID = addDocument(obj);
        jedis.setex(cacheID + identifier, 3600, obj.put("_id", objectID.getValue().toString()).toString());
    }

    @SneakyThrows
    public void updateCache(String identifier, Document oldDoc,  Document document) {
        jedis.del(identifier);
        jedis.setex(cacheID + identifier, 3600, documentToJSON(document));
        upsertDocument(oldDoc, document);
    }

    @SneakyThrows
    public void updateCache(String identifier, Document document) {
        jedis.del(identifier);
        jedis.setex(cacheID + identifier, 3600, documentToJSON(document));
        upsertDocument(document);
    }

    @SneakyThrows
    public void updateCache(String identifier, JSONObject object) {
        jedis.del(identifier);
        jedis.setex(cacheID + identifier, 3600, object.toString());
        upsertDocument(object);
    }

    public void updateCacheObjects(HashMap<String, JSONObject> objects) {
        final HashMap<String, Document> documents = new HashMap<>();
        objects.forEach((key, object) -> documents.put(key, Document.parse(object.toString())));
        updateCache(documents);
    }

    public void updateCache(HashMap<String, Document> documents) {

        for (var document : documents.entrySet()) {
            jedis.del(document.getKey());
            jedis.setex(cacheID + document.getKey(), 3600, documentToJSON(document.getValue()));
        }

        upsertManyDocuments(documents.values().stream().toList());
    }

    public <T> void updateCache(JSONObject obj, GenericJSONField identifier, T identifierValue) {
        updateCache(obj, identifier.toString(), identifierValue);
    }

    public <T> void updateCache(JSONObject obj, String identifier, T identifierValue) {
        if (!obj.has(identifier))
            throw new IllegalArgumentException("The JSON object must have the identifier passed!");

        Document document = findSpecificDocument(identifier, identifierValue);

        if (document == null)
            throw new NullPointerException("There was no document found with that identifier value!");

        updateCache(identifierValue.toString(), document, Document.parse(obj.toString()));
    }

    public JSONObject getCacheJSON(String identifier) {
        return new JSONObject(get(identifier));
    }

    public JSONObject getJSON(String id) {
        return new JSONObject(get(cacheID + id));
    }

    public JSONObject getJSONByGuild(String gid) {
        return getJSON(gid);
    }

    private JSONObject collectionToJSON(MongoCollection<Document> collection) {
        JSONObject collectionObj = new JSONObject();
        JSONArray documentArr = new JSONArray();

        collection.find().forEach(doc -> documentArr.put(new JSONObject(doc.toJson())));

        collectionObj.put(CacheField.DOCUMENTS.toString(), documentArr);
        return collectionObj;
    }

    public JSONObject getCache(String id) {
        return new JSONObject(get(cacheID + id));
    }

    enum CacheField implements GenericJSONField {
        DOCUMENTS("documents"),
        GUILD_ID("guild_id"),
        ID("_id");

        private final String str;

        CacheField(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
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

    public enum Fields implements GenericJSONField {
        DOCUMENTS("documents");

        private final String str;

        Fields(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
