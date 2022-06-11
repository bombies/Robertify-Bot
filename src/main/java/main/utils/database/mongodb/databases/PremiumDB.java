package main.utils.database.mongodb.databases;

import main.constants.Database;
import main.utils.database.mongodb.AbstractMongoDatabase;

public class PremiumDB extends AbstractMongoDatabase {
    private static PremiumDB instance;

    public PremiumDB() {
        super(Database.Mongo.ROBERTIFY_DATABASE, Database.Mongo.ROBERTIFY_PREMIUM);
    }

    @Override
    public void init() {

    }

    public synchronized static PremiumDB ins() {
        if (instance == null)
            instance = new PremiumDB();
        return instance;
    }

    public enum Field {
        USER_ID("user_id"),
        PREMIUM_TYPE("premium_type"),
        PREMIUM_TIER("premium_tier"),
        PREMIUM_SERVERS("premium_servers"),
        PREMIUM_STARTED("premium_started"),
        PREMIUM_EXPIRES("premium_expires");

        private final String str;

        Field(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return this.str;
        }
    }
}
