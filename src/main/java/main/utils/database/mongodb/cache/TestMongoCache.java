package main.utils.database.mongodb.cache;

import lombok.Getter;
import lombok.Setter;
import main.utils.database.mongodb.MongoTestDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(TestMongoCache.class);
    @Getter @Setter
    private static AbstractMongoCache<MongoTestDB> cache = null;

    public String cacheTest() {
        return cache.getJSON().toString(4);
    }
}
