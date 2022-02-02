package main.utils.database.mongodb.cache;

import com.mongodb.client.MongoCollection;
import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.databases.GuildsDB;
import main.utils.json.AbstractJSON;
import main.utils.json.GenericJSONField;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AbstractMongoCache extends AbstractMongoDatabase implements AbstractJSON {
    @Getter
    private final AbstractMongoDatabase mongoDB;
    @Getter
    private MongoCollection<Document> collection;
    private JSONObject cache;

    AbstractMongoCache(AbstractMongoDatabase mongoDB) {
        super(mongoDB);
        this.mongoDB = mongoDB;
        this.collection = mongoDB.getCollection();
        this.cache = new JSONObject().put("documents", new JSONArray());
    }

    @Override
    public void init() {
        mongoDB.init();
    }

    public void addToCache(Document document) {
        addDocument(document);
        updateCache();
    }

    public void addToCache(JSONObject object) {
        addToCache(Document.parse(object.toString()));
    }

    public <T> void removeFromCache(GenericJSONField identifier, T identifierValue) {
        getCache().remove(getIndexOfObjectInArray(getCache(), identifier, identifierValue));
        removeSpecificDocument(identifier.toString(), identifierValue);
    }

    public void updateCache() {
        this.collection = mongoDB.getCollection();
        this.cache = collectionToJSON(this.collection);
    }

    @SneakyThrows
    public void updateCache(MongoCollection<Document> collection) {
        this.collection = collection;
        this.cache = collectionToJSON(collection);
    }

    @SneakyThrows
    public void updateCache(Document document) {
        final ObjectId id = document.getObjectId("_id");
        final JSONArray collectionArr = this.cache.getJSONArray(CacheField.DOCUMENTS.toString());

        collectionArr.remove(getIndexOfObjectInArray(collectionArr, id));
        collectionArr.put(new JSONObject(document.toJson()));

        upsertDocument(document);
    }

    public void updateCacheObjects(List<JSONObject> objects) {
        final List<Document> documents = new ArrayList<>();
        objects.forEach(object -> documents.add(Document.parse(object.toString())));
        updateCache(documents);
    }

    public void updateCache(List<Document> documents) {

        for (var document: documents) {
            final ObjectId id = document.getObjectId("_id");
            final JSONArray collectionArr = this.cache.getJSONArray(CacheField.DOCUMENTS.toString());

            collectionArr.remove(getIndexOfObjectInArray(collectionArr, id));
            collectionArr.put(new JSONObject(document.toJson()));
        }

        upsertManyDocuments(documents);
    }

    public <T> void updateCache(JSONObject obj, GenericJSONField identifier, T identifierValue) {
        updateCache(obj, identifier.toString(), identifierValue);
    }

    public void updateGuild(JSONObject obj, long gid) {
        updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
    }

    public <T> void updateCache(JSONObject obj, String identifier, T identifierValue) {
        if (!obj.has(identifier))
            throw new IllegalArgumentException("The JSON object must have the identifier passed!");

        Document document = findSpecificDocument(identifier, identifierValue);

        if (document == null)
            throw new NullPointerException("There was no document found with that identifier value!");

        updateCache(Document.parse(obj.toString()));
    }

    public JSONObject getCacheJSON() {
        return cache;
    }

    public JSONObject getJSON(String id) {
        var arr = cache.getJSONArray(CacheField.DOCUMENTS.toString());
        return arr.getJSONObject(getIndexOfObjectInArray(arr, CacheField.ID, id));
    }

    public JSONObject getJSONByGuild(String gid) {
        var arr = cache.getJSONArray(CacheField.DOCUMENTS.toString());
        return arr.getJSONObject(getIndexOfObjectInArray(arr, CacheField.GUILD_ID, gid));
    }

    private JSONObject collectionToJSON(MongoCollection<Document> collection) {
        JSONObject collectionObj = new JSONObject();
        JSONArray documentArr = new JSONArray();

        collection.find().forEach(doc -> documentArr.put(new JSONObject(doc.toJson())));

        collectionObj.put(CacheField.DOCUMENTS.toString(), documentArr);
        return collectionObj;
    }

    public JSONArray getCache() {
        return cache.getJSONArray(CacheField.DOCUMENTS.toString());
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
