package main.utils.database.mongodb.cache;

import main.utils.database.mongodb.MongoTestDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TestMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(TestMongoCache.class);
    public static AbstractMongoCache<MongoTestDB> TEST_CACHE = null;

    public static String cacheTest(AbstractMongoCache<MongoTestDB> cache) {
        return cache.getJSON().toString(4);
    }
}
