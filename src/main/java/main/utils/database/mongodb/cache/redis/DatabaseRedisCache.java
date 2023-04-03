package main.utils.database.mongodb.cache.redis;

import com.mongodb.client.MongoCollection;
import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.json.AbstractJSON;
import main.utils.json.GenericJSONField;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class DatabaseRedisCache extends RedisCache implements AbstractJSON {
    @Getter
    private final AbstractMongoDatabase mongoDB;
    @Getter
    private final MongoCollection<Document> collection;

    protected DatabaseRedisCache(String cacheID, AbstractMongoDatabase mongoDB) {
        super(cacheID);
        this.mongoDB = mongoDB;
        this.collection = mongoDB.getCollection();
    }

    public void init() {
        mongoDB.init();
    }

    public void addToCache(String identifier, JSONObject obj) {
        final var objectID = mongoDB.addDocument(obj);
        setex( identifier, 3600, obj.put("_id", objectID.getValue().toString()).toString());
    }

    @SneakyThrows
    public void updateCache(String identifier, Document oldDoc,  Document document) {
        del(identifier);
        setex(identifier, 3600, mongoDB.documentToJSON(document));
        mongoDB.upsertDocument(oldDoc, document);
    }

    @SneakyThrows
    public void updateCache(String identifier, Document document) {
        del(identifier);
        setex(identifier, 3600, mongoDB.documentToJSON(document));
        mongoDB.upsertDocument(document);
    }

    @SneakyThrows
    public void updateCacheNoDB(String identifier, Document document) {
        del(identifier);
        setex(identifier, 3600, mongoDB.documentToJSON(document));
    }

    @SneakyThrows
    public void updateCacheNoDB(String identifier, JSONObject json) {
        del(identifier);
        setex(identifier, 3600, json.toString());
    }

    @SneakyThrows
    public void updateCache(String identifier, JSONObject object) {
        del(identifier);
        setex(identifier, 3600, object.toString());
        mongoDB.upsertDocument(object);
    }

    public void updateCacheObjects(HashMap<String, JSONObject> objects) {
        final HashMap<String, Document> documents = new HashMap<>();
        objects.forEach((key, object) -> documents.put(key, Document.parse(object.toString())));
        updateCache(documents);
    }

    public void updateCache(HashMap<String, Document> documents) {

        for (var document : documents.entrySet()) {
            del(document.getKey());
            setex(document.getKey(), 3600, mongoDB.documentToJSON(document.getValue()));
        }

        mongoDB.upsertManyDocuments(documents.values().stream().toList());
    }

    public <T> void updateCache(JSONObject obj, GenericJSONField identifier, T identifierValue) {
        updateCache(obj, identifier.toString(), identifierValue);
    }

    public <T> void updateCache(JSONObject obj, String identifier, T identifierValue) {
        if (!obj.has(identifier))
            throw new IllegalArgumentException("The JSON object must have the identifier passed!");

        Document document = mongoDB.findSpecificDocument(identifier, identifierValue);

        if (document == null)
            throw new NullPointerException("There was no document found with that identifier value!");

        updateCache(identifierValue.toString(), document, Document.parse(obj.toString()));
    }

    public JSONObject getJSON(String id) {
        return new JSONObject(get(id));
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
        return new JSONObject(get(id));
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
