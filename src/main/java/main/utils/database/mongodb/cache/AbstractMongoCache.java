package main.utils.database.mongodb.cache;

import main.utils.database.mongodb.AbstractMongoDatabase;

public abstract class AbstractMongoCache<E extends AbstractMongoDatabase> {
    private final E database;
    private MongoCache cache;


    public AbstractMongoCache(E database) {
        this.database = database;
        cache = new MongoCache(this.database);
    }

    public static void getCache() {
        return;
    }
}
