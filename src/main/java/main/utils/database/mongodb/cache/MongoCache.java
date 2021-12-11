package main.utils.database.mongodb.cache;

import com.mongodb.client.MongoCollection;
import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.database.mongodb.AbstractMongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

class MongoCache {
    private static final List<MongoCache> caches = new ArrayList<>();

    @Getter
    private final AbstractMongoDatabase mongoDB;
    @Getter
    private final List<MongoCollection<Document>> collections = new ArrayList<>();

    MongoCache(AbstractMongoDatabase mongoDB) {
        this.mongoDB = mongoDB;

        mongoDB.getDatabase().listCollectionNames()
                .forEach(collectionName ->
                        collections.add(mongoDB.getDatabase().getCollection(collectionName)));
    }

    public static void updateCache(AbstractMongoDatabase db) {
        caches.add(new MongoCache(db));
    }

    @SneakyThrows
    public static void updateCache(AbstractMongoDatabase db, MongoCollection<Document>... collections) {
        var cache = getCache(db);
        var cacheClone = (MongoCache) cache.clone();

        UnaryOperator<MongoCollection<Document>> replaceAll = collection -> {
            for (MongoCollection<Document> col : collections)
                if (col.getNamespace().getCollectionName().equals(collection.getNamespace().getCollectionName()))
                    return col;
            return collection;
        };

        cacheClone.collections.replaceAll(replaceAll);
        caches.remove(cache);
        caches.add(cacheClone);
    }

    @SneakyThrows
    public static void updateCache(AbstractMongoDatabase db, MongoCollection<Document> collection, Document document) {
        var cache = getCache(db);
        var cacheClone = (MongoCache) cache.clone();

        MongoCollection<Document> cacheCollection = cache.getCollections().stream().filter(col -> col.equals(collection))
                .findFirst().orElse(null);

        if (cacheCollection == null)
            throw new NullPointerException("Invalid collection");

        Document oldDoc = db.findSpecificDocument("_id", (String) document.get("_id"));

        if (oldDoc == null)
            throw new NullPointerException("No such document found in collection");

        cacheCollection.updateOne(oldDoc, document);

        caches.remove(cache);
        caches.add(cacheClone);
    }

    private static MongoCache getCache(AbstractMongoDatabase db) {
        return caches.stream()
                .filter(cache -> cache.getMongoDB().equals(db))
                .findFirst().orElse(null);
    }
}
