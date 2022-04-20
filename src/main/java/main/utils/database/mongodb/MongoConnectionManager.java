package main.utils.database.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import main.constants.Database;
import main.constants.ENV;
import main.main.Config;

public class MongoConnectionManager {

    private static MongoConnectionManager INSTANCE;
    private final MongoClient client;
    private MongoDatabase database;

    private MongoConnectionManager() {
        final ConnectionString CONNECTION_STRING = new ConnectionString(Database.Mongo.getConnectionString(Config.get(ENV.MONGO_DATABASE_NAME)));
        final MongoClientSettings CLIENT_SETTINGS = MongoClientSettings.builder()
                .applyConnectionString(CONNECTION_STRING)
                .build();

        this.client = MongoClients.create(CLIENT_SETTINGS);
    }

    public MongoConnectionManager connect(Database.Mongo database) {
        try {
            this.database = client.getDatabase(database.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("There was no database with the name " + database);
        }

        return this;
    }

    public MongoDatabase database() {
        if (database == null)
            throw new NullPointerException("There is no connected database!");
        return database;
    }

    public static MongoConnectionManager ins() {
        if (INSTANCE == null)
            INSTANCE = new MongoConnectionManager();
        return INSTANCE;
    }
}
