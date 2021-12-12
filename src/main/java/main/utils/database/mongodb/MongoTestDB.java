package main.utils.database.mongodb;

import main.constants.Database;
import main.utils.database.mongodb.cache.MongoCache;
import org.bson.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoTestDB extends AbstractMongoDatabase {
    private final static Logger logger = LoggerFactory.getLogger(MongoTestDB.class);


    public MongoTestDB() {
        super(Database.MONGO.ROBERTIFY_DATABASE, Database.MONGO.ROBERTIFY_TEST);
    }


    public void addItem(String guild_id, long dummy_time, JSONObject dummyObj) {
        Document doc = DocumentBuilder.create()
                .addField("guild_id", guild_id)
                .addField("time", dummy_time)
                .addField("object", dummyObj)
                .build();

        addDocument(doc);
        updateCache();
    }

    public void removeItem(String guild_id) {
        removeDocument("guild_id", guild_id);
        updateCache();
    }

    public <T> void updateItem(String guildID, String key, T value) {
        updateDocument("guild_id", guildID, key, value);
        updateCache();
    }

    public <T> String getItemString(String key, T value, boolean indented) {
        return getDocument(key, value, indented);
    }

    public <T> String getItemsString(String key, T value) {
        return getDocuments(key, value);
    }

    private void updateCache() {
        MongoCache.TEST_CACHE.updateCache();
        logger.info("Updated test cache");
    }

    @Override
    public void init() {

    }
}
