package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.databases.MongoTestDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMongoCache extends AbstractMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(TestMongoCache.class);
    @Getter
    private static TestMongoCache instance;

    private TestMongoCache() {
        super(MongoTestDB.ins());
    }

    public String cacheTest() {
        return getCache().toString(4);
    }

    public static void initCache() {
        instance = new TestMongoCache();
    }
}
