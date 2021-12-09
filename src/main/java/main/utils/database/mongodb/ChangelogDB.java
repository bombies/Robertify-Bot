package main.utils.database.mongodb;

import main.constants.Database;

public class ChangelogDB extends AbstractMongoDatabase {
    ChangelogDB(Database.MONGO db, Database.MONGO collection) {
        super(db, collection);
    }
}
