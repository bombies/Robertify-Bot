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
    private static final HashMap<Class, AbstractMongoCache> INSTANCES = new HashMap<>();

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

    @SneakyThrows
    public void updateCache(MongoCollection<Document> collection) {
        this.collection = collection;
        this.collectionJSONCache = collectionToJSON(collection);
    }

    @SneakyThrows
    public void updateCache(Document document) {
        final String id = (String) document.get("_id");
        final JSONArray collectionArr = this.collectionJSONCache.getJSONArray(CacheField.DOCUMENTS.toString());

        collectionArr.remove(getIndexOfObjectInArray(collectionArr, CacheField.DOCUMENTS, id));
        collectionArr.put(new JSONObject(document.toJson()));

        upsertDocument(document);
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

    static <T extends AbstractMongoDatabase> AbstractMongoCache<T> ins(T db) {
        if (INSTANCES.containsKey(db.getClass())) {
            return INSTANCES.get(db.getClass());
        } else {
            AbstractMongoCache<T> abstractMongoCache = new AbstractMongoCache<>(db);
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
        DOCUMENTS("documents");

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
