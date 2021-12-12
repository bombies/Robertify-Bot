package main.utils.database.mongodb.cache;

import com.mongodb.client.MongoCollection;
import main.utils.database.mongodb.AbstractMongoDatabase;
import org.bson.Document;
import org.json.JSONObject;

public class TestMongoCache extends AbstractMongoCache<AbstractMongoDatabase> {
    private AbstractMongoDatabase database;
    private AbstractMongoCache<AbstractMongoDatabase> cache;

    private TestMongoCache(AbstractMongoDatabase database) {
        super(database);
        this.database = database;
        cache = AbstractMongoCache.ins(database);
    }

    public void updateCache(JSONObject obj) {
        updateCache(Document.parse(obj.toString()));
    }
}
