package main.utils.database.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.ConnectionString;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import main.constants.Database;
import main.constants.ENV;
import main.events.MongoEventListener;
import main.main.Config;
import main.utils.database.mongodb.cache.AbstractMongoCache;
import main.utils.database.mongodb.cache.TestMongoCache;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractMongoDatabase {
    private final Logger logger = LoggerFactory.getLogger(AbstractMongoDatabase.class);

    private final MongoClient client;
    @Getter
    private final MongoDatabase database;
    @Getter
    private MongoCollection<Document> collection;

    public AbstractMongoDatabase(Database.MONGO db) {
        final ConnectionString CONNECTION_STRING = new ConnectionString(Database.MONGO.getConnectionString(Config.get(ENV.MONGO_DATABASE_NAME)));
        final MongoClientSettings CLIENT_SETTINGS = MongoClientSettings.builder()
                .applyConnectionString(CONNECTION_STRING)
                .build();

        client = MongoClients.create(CLIENT_SETTINGS);

        try {
            database = client.getDatabase(db.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("There was no database with the name " + db);
        }

        this.collection = null;
    }

    public AbstractMongoDatabase(AbstractMongoDatabase db) {
        this.client = db.client;
        this.database = db.database;
        this.collection = db.collection;
    }

    public AbstractMongoDatabase(Database.MONGO db, Database.MONGO collection) {
        final ConnectionString CONNECTION_STRING = new ConnectionString(Database.MONGO.getConnectionString(Config.get(ENV.MONGO_DATABASE_NAME)));
        final MongoClientSettings CLIENT_SETTINGS = MongoClientSettings.builder()
                .applyConnectionString(CONNECTION_STRING)
                .addCommandListener(new MongoEventListener())
                .build();

        client = MongoClients.create(CLIENT_SETTINGS);

        try {
            database = client.getDatabase(db.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("There was no database with the name " + db);
        }

        try {
            this.collection = database.getCollection(collection.toString());
        } catch (IllegalArgumentException e) {
            database.createCollection(collection.toString());
            logger.info("Created a new collection with name \"{}\" in {}", collection, db);
            this.collection = database.getCollection(collection.toString());
        }
    }

    public abstract void init();

    public static void initAllCaches() {
        var testDb = new MongoTestDB();

        TestMongoCache.setCache((AbstractMongoCache<MongoTestDB>) AbstractMongoCache.ins(testDb));
    }

    MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name);
    }

    protected void addDocument(Document doc) {
        collection.insertOne(doc);
    }

    protected void upsertDocument(Document doc) {
        String id = doc.getString("_id");

        Document oldDoc = null;
        for (var document : collection.find())
            if (document.getString("_id").equals(id))
                oldDoc = document;

        if (oldDoc == null)
            addDocument(doc);
        else
            updateDocument(oldDoc, doc);
    }

    protected void upsertDocument(JSONObject obj) {
        upsertDocument(Document.parse(obj.toString()));
    }

    protected void addDocument(JSONObject obj) {
        collection.insertOne(Document.parse(obj.toString()));
    }

    protected void removeDocument(Document doc) {
        collection.deleteOne(doc);
    }

    protected void removeDocument(JSONObject obj) {
        collection.deleteOne(Document.parse(obj.toString()));
    }

    /**
     * Remove all documents with the specified key and value
     * @param key The key
     * @param value The value attached to the key
     */
    protected void removeDocument(String key, String value) {
        if (!documentExists(key, value))
            throw new NullPointerException("There is no such document found with key: " + key);

        var iterator = findDocument(key, value);
        while (iterator.hasNext())
            removeDocument(iterator.next());
    }

    protected void removeSpecificDocument(String key, String value) {
        if (!documentExists(key, value))
            throw new NullPointerException("There is no such document found with key: " + key);

        var doc = findDocument(key, value).next();
        removeDocument(doc);
    }

    protected void updateDocument(Document document, Bson updates) {
        UpdateOptions options = new UpdateOptions().upsert(true);
        collection.updateOne(document, updates, options);
    }

    protected void updateDocument(Document document, List<Bson> updates) {
        UpdateOptions options = new UpdateOptions().upsert(true);
        collection.updateOne(document, updates, options);
    }

    protected <T> void updateDocument(Document document, String key, T newValue) {
        Bson updates = Updates.combine(
                Updates.set(key, newValue),
                Updates.currentTimestamp("lastUpdated")
        );

        updateDocument(document, updates);
    }

    protected void updateDocument(Document document, JSONObject json) {
        final List<Bson> updates = new ArrayList<>();

        for (String key : json.keySet())
            updates.add(Updates.combine(
                    Updates.set(key, json.get(key)),
                    Updates.currentTimestamp("lastUpdated")
            ));

        updateDocument(document, updates);
    }

    protected <T> void updateDocument(String idName, String idValue, String key, T newValue) {
        Document document = findSpecificDocument(idName, idValue);

        if (document == null)
            throw new NullPointerException("There is no such document with mapping <"+idName+":"+idValue+">");

        updateDocument(document, key, newValue);
    }

    protected boolean documentExists(String key, String value) {
        return findDocument(key, value).hasNext();
    }

    protected boolean documentExists(String key, Document value) {
        return findDocument(key, value).hasNext();
    }

    protected boolean documentExists(String key, JSONObject value) {
        return documentExists(key, Document.parse(value.toString()));
    }

    protected Iterator<Document> findDocument(String key, String value) {
        return collection.find(eq(key, value)).iterator();
    }

    protected Document findSpecificDocument(String key, String value) {
        return collection.find(eq(key, value)).iterator().next();
    }

    protected Document findSpecificDocument(String key, Document value) {
        return collection.find(eq(key, value)).iterator().next();
    }

    protected Document findSpecificDocument(String key, JSONObject value) {
        return collection.find(eq(key, value)).iterator().next();
    }

    protected Iterator<Document> findDocument(String key, Document value) {
        return collection.find(eq(key, value)).iterator();
    }

    protected Iterator<Document> findDocument(String key, JSONObject value) {
        return findDocument(key, Document.parse(value.toString()));
    }

    protected Iterator<Document> findDocument(Document doc) {
        return collection.find(doc).iterator();
    }

    protected Iterator<Document> findDocument(JSONObject obj) {
        return findDocument(Document.parse(obj.toString()));
    }

    protected <T> String getDocument(String key, T value) {
        return getDocument(key, value, false);
    }

    protected <T> String getDocument(String key, T value, boolean indented) {
        Document doc;

        if (value instanceof String s)
            doc = findSpecificDocument(key, s);
        else if (value instanceof Document d)
            doc = findSpecificDocument(key, d);
        else if (value instanceof JSONObject j)
            doc = findSpecificDocument(key, j);
        else
            throw new IllegalArgumentException("Invalid value type!");

        if (doc == null)
            throw new NullPointerException("There was no such document with key \""+key+"\" and value \""+value+"\"");

        return documentToJSON(doc, indented);
    }

    protected <T> String getDocuments(String key, T value) {
        return getDocuments(key, value, false);
    }

    protected <T> String getDocuments(String key, T value, boolean indented) {
        Iterator<Document> doc;

        if (value instanceof String s)
            doc = findDocument(key, s);
        else if (value instanceof Document d)
            doc = findDocument(key, d);
        else if (value instanceof JSONObject j)
            doc = findDocument(key, j);
        else
            throw new IllegalArgumentException("Invalid value type!");

        if (doc == null)
            throw new NullPointerException("There was no such document with key \""+key+"\" and value \""+value+"\"");

        StringBuilder sb = new StringBuilder();

        while (doc.hasNext())
            sb.append(documentToJSON(doc.next(), indented)).append("\n");

        return sb.toString();
    }

    protected String documentToJSON(Document doc, boolean indented) {
        return doc.toJson(JsonWriterSettings.builder()
                .indent(indented)
                .build()
        );
    }

    protected String documentToJSON(Document doc) {
        return documentToJSON(doc, false);
    }

    private <TItem> Bson eq(String key, TItem value) {
        return Filters.eq(key, value);
    }
}
