package main.utils.database.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.ConnectionString;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import main.constants.Database;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractMongoDatabase {
    @Getter
    private final MongoClient client;
    @Getter
    private final MongoDatabase database;
    @Getter
    private final MongoCollection<Document> collection;

    public AbstractMongoDatabase(Database.MONGO db, Database.MONGO collection) {
        final ConnectionString CONNECTION_STRING = new ConnectionString(Database.MONGO.getConnectionString("myFirstDatabase"));
        final MongoClientSettings CLIENT_SETTINGS = MongoClientSettings.builder()
                .applyConnectionString(CONNECTION_STRING)
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
            throw new NullPointerException("There is no collection in " + db + " called " + collection);
        }
    }

    void addDocument(Document doc) {
        getCollection().insertOne(doc);
    }

    void addDocument(JSONObject obj) {
        getCollection().insertOne(Document.parse(obj.toString()));
    }

    void removeDocument(Document doc) {
        getCollection().deleteOne(doc);
    }

    void removeDocument(JSONObject obj) {
        getCollection().deleteOne(Document.parse(obj.toString()));
    }

    /**
     * Remove all documents with the specified key and value
     * @param key The key
     * @param value The value attached to the key
     */
    void removeDocument(String key, String value) {
        if (!documentExists(key, value))
            throw new NullPointerException("There is no such document found with key: " + key);

        var iterator = findDocument(key, value);
        while (iterator.hasNext())
            removeDocument(iterator.next());
    }

    void removeSpecificDocument(String key, String value) {
        if (!documentExists(key, value))
            throw new NullPointerException("There is no such document found with key: " + key);

        var doc = findDocument(key, value).next();
        removeDocument(doc);
    }

    void updateDocument(Document document, Bson updates) {
        UpdateOptions options = new UpdateOptions().upsert(true);
        getCollection().updateOne(document, updates, options);
    }

    void updateDocument(Document document, List<Bson> updates) {
        UpdateOptions options = new UpdateOptions().upsert(true);
        getCollection().updateOne(document, updates, options);
    }

    <T> void updateDocument(Document document, String key, T newValue) {
        Bson updates = Updates.combine(
                Updates.set(key, newValue),
                Updates.currentTimestamp("lastUpdated")
        );

        updateDocument(document, updates);
    }

    void updateDocument(Document document, JSONObject json) {
        final List<Bson> updates = new ArrayList<>();

        for (String key : json.keySet())
            updates.add(Updates.combine(
                    Updates.set(key, json.get(key)),
                    Updates.currentTimestamp("lastUpdated")
            ));

        updateDocument(document, updates);
    }

    <T> void updateDocument(String idName, String idValue, String key, T newValue) {
        Document document = findSpecificDocument(idName, idValue);

        if (document == null)
            throw new NullPointerException("There is no such document with mapping <"+idName+":"+idValue+">");

        updateDocument(document, key, newValue);
    }

    boolean documentExists(String key, String value) {
        return findDocument(key, value).hasNext();
    }

    boolean documentExists(String key, Document value) {
        return findDocument(key, value).hasNext();
    }

    public boolean documentExists(String key, JSONObject value) {
        return documentExists(key, Document.parse(value.toString()));
    }

    Iterator<Document> findDocument(String key, String value) {
        return getCollection().find(eq(key, value)).iterator();
    }

    Document findSpecificDocument(String key, String value) {
        return getCollection().find(eq(key, value)).iterator().next();
    }

    Document findSpecificDocument(String key, Document value) {
        return getCollection().find(eq(key, value)).iterator().next();
    }

    Document findSpecificDocument(String key, JSONObject value) {
        return getCollection().find(eq(key, value)).iterator().next();
    }

    Iterator<Document> findDocument(String key, Document value) {
        return getCollection().find(eq(key, value)).iterator();
    }

    Iterator<Document> findDocument(String key, JSONObject value) {
        return findDocument(key, Document.parse(value.toString()));
    }

    Iterator<Document> findDocument(Document doc) {
        return getCollection().find(doc).iterator();
    }

    Iterator<Document> findDocument(JSONObject obj) {
        return findDocument(Document.parse(obj.toString()));
    }

    <T> String getDocument(String key, T value) {
        return getDocument(key, value, false);
    }

    <T> String getDocument(String key, T value, boolean indented) {
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

    <T> String getDocuments(String key, T value) {
        return getDocuments(key, value, false);
    }

    <T> String getDocuments(String key, T value, boolean indented) {
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

    String documentToJSON(Document doc, boolean indented) {
        return doc.toJson(JsonWriterSettings.builder()
                .indent(indented)
                .build()
        );
    }

    String documentToJSON(Document doc) {
        return documentToJSON(doc, false);
    }

    <TItem> Bson eq(String key, TItem value) {
        return Filters.eq(key, value);
    }
}
