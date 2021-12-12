package main.utils.database.mongodb.cache;

import com.mongodb.client.MongoCollection;
import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.json.IJSONField;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class AbstractMongoCache<T extends AbstractMongoDatabase> extends AbstractMongoDatabase {
    private static final HashMap<Class<? extends AbstractMongoDatabase>, AbstractMongoCache<AbstractMongoDatabase>> INSTANCES = new HashMap<>();

    @Getter
    private final T mongoDB;
    @Getter
    private MongoCollection<Document> collection;
    private JSONObject collectionJSONCache;

    AbstractMongoCache(T mongoDB) {
        super(mongoDB);
        this.mongoDB = mongoDB;
        this.collection = mongoDB.getCollection();
        this.collectionJSONCache = collectionToJSON(this.collection);
    }

    @Override
    public void init() {
        // NOTHING
    }

    public void updateCache() {
        this.collection = mongoDB.getCollection();
        this.collectionJSONCache = collectionToJSON(this.collection);
    }

    @SneakyThrows
    public void updateCache(MongoCollection<Document> collection) {
        this.collection = collection;
        this.collectionJSONCache = collectionToJSON(collection);
    }

    @SneakyThrows
    public void updateCache(Document document) {
        final String id = (String) document.get("_id");
        final JSONArray collectionArr = this.collectionJSONCache.getJSONArray(CacheField.DOCUMENTS.toString());

        collectionArr.remove(getIndexOfObjectInArray(collectionArr, CacheField.ID, id));
        collectionArr.put(new JSONObject(document.toJson()));

        upsertDocument(document);
    }

    public JSONObject getJSON() {
        return collectionJSONCache;
    }

    public JSONObject getJSON(String id) {
        var arr = collectionJSONCache.getJSONArray(CacheField.DOCUMENTS.toString());
        return arr.getJSONObject(getIndexOfObjectInArray(arr, CacheField.ID, id));
    }

    public JSONObject getJSONByGuild(String gid) {
        var arr = collectionJSONCache.getJSONArray(CacheField.DOCUMENTS.toString());
        return arr.getJSONObject(getIndexOfObjectInArray(arr, CacheField.GUILD_ID, gid));
    }

    private int getIndexOfObjectInArray(JSONArray array, IJSONField field, Object object) {
        if (!arrayHasObject(array, field, object))
            throw new NullPointerException("There was no such object found in the array!");

        for (int i = 0; i < array.length(); i++)
            if (array.getJSONObject(i).get(field.toString()).equals(object))
                return i;
        return -1;
    }

    private boolean arrayHasObject(JSONArray array, IJSONField field, Object object) {
        for (int i = 0; i < array.length(); i++)
            if (array.getJSONObject(i).get(field.toString()).equals(object))
                return true;
        return false;
    }

    static AbstractMongoCache<AbstractMongoDatabase> ins(AbstractMongoDatabase db) {
        if (INSTANCES.containsKey(db.getClass())) {
            return INSTANCES.get(db.getClass());
        } else {
            AbstractMongoCache<AbstractMongoDatabase> abstractMongoCache = new AbstractMongoCache<>(db);
            INSTANCES.put(db.getClass(), abstractMongoCache);
            return abstractMongoCache;
        }
    }

    private JSONObject collectionToJSON(MongoCollection<Document> collection) {
        JSONObject collectionObj = new JSONObject();
        JSONArray documentArr = new JSONArray();

        collection.find().forEach(doc -> documentArr.put(new JSONObject(doc.toJson())));

        collectionObj.put(CacheField.DOCUMENTS.toString(), documentArr);
        return collectionObj;
    }

    enum CacheField implements IJSONField {
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
}
