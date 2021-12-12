package main.utils.database.mongodb.cache;

import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.MongoTestDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MongoCache {
    private final static Logger logger = LoggerFactory.getLogger(MongoCache.class);
    public static AbstractMongoCache<AbstractMongoDatabase> TEST_CACHE = null;


    public static void initAllCaches() {
        var testDb = new MongoTestDB();

        TEST_CACHE = AbstractMongoCache.ins(testDb);
    }

    public static String cacheTest(AbstractMongoCache<AbstractMongoDatabase> cache) {
        return cache.getJSON().toString(4);
    }
}
