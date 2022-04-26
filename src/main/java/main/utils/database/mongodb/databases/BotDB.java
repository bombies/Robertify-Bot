package main.utils.database.mongodb.databases;

import main.constants.Database;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.DocumentBuilder;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.GenericJSONField;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotDB extends AbstractMongoDatabase {
    private final static Logger logger = LoggerFactory.getLogger(BotDB.class);
    private final static BotDB INSTANCE = new BotDB();

    private BotDB() {
        super(Database.Mongo.ROBERTIFY_DATABASE, Database.Mongo.ROBERTIFY_BOT_INFO);
    }

    @Override
    public synchronized void init() {
        if (getCollection().countDocuments() == 0) {
            addDocument(
                    DocumentBuilder.create()
                            .addField("identifier", "robertify_main_config")
                            .addField(Fields.LAST_BOOTED, 0L)
                            .addField(Fields.SUGGESTIONS_OBJECT, new JSONObject()
                                    .put(Fields.SubFields.SUGGESTIONS_CATEGORY.toString(), -1L)
                                    .put(Fields.SubFields.SUGGESTIONS_PENDING_CHANNEL.toString(), -1L)
                                    .put(Fields.SubFields.SUGGESTIONS_ACCEPTED_CHANNEL.toString(), -1L)
                                    .put(Fields.SubFields.SUGGESTIONS_DENIED_CHANNEL.toString(), -1L)
                                    .put(Fields.SubFields.SUGGESTIONS_BANNED_USERS.toString(), new JSONArray())
                            )
                            .addField(Fields.REPORTS_OBJECT, new JSONObject()
                                    .put(Fields.SubFields.REPORTS_CATEGORY.toString(), -1L)
                                    .put(Fields.SubFields.REPORTS_CHANNEL.toString(), -1)
                                    .put(Fields.SubFields.REPORTS_BANNED_USERS.toString(), new JSONArray())
                            )
                            .addField(Fields.DEVELOPERS_ARRAY, new JSONArray())
                            .addField(Fields.LATEST_ALERT, new JSONObject())
                            .addField(Fields.ALERT_VIEWERS, new JSONArray())
                            .build()
            );
        }
    }

    public static void update() {
        logger.debug("Updating Bot Info cache");
        var cache = BotBDCache.getInstance();

        for (var obj : cache.getCache()) {
            final JSONObject jsonObject = (JSONObject) obj;
            boolean changesMade = false;

            for (var fields : Fields.values()) {
                if (jsonObject.has(fields.toString()))
                    continue;

                changesMade = true;
                switch (fields) {
                    case LAST_BOOTED -> jsonObject.put(Fields.LAST_BOOTED.toString(), System.currentTimeMillis());

                    case REPORTS_OBJECT -> jsonObject.put(Fields.REPORTS_OBJECT.toString(), new JSONObject()
                            .put(Fields.SubFields.REPORTS_CATEGORY.toString(), -1L)
                            .put(Fields.SubFields.REPORTS_CHANNEL.toString(), -1)
                            .put(Fields.SubFields.REPORTS_BANNED_USERS.toString(), -1)
                    );
                    case SUGGESTIONS_OBJECT -> jsonObject.put(Fields.SUGGESTIONS_OBJECT.toString(), new JSONObject()
                            .put(Fields.SubFields.SUGGESTIONS_CATEGORY.toString(), -1L)
                            .put(Fields.SubFields.SUGGESTIONS_PENDING_CHANNEL.toString(), -1L)
                            .put(Fields.SubFields.SUGGESTIONS_ACCEPTED_CHANNEL.toString(), -1L)
                            .put(Fields.SubFields.SUGGESTIONS_DENIED_CHANNEL.toString(), -1L)
                            .put(Fields.SubFields.SUGGESTIONS_BANNED_USERS.toString(), -1L)
                    );
                    case DEVELOPERS_ARRAY -> jsonObject.put(Fields.DEVELOPERS_ARRAY.toString(), new JSONArray());
                    case RANDOM_MESSAGES -> jsonObject.put(Fields.RANDOM_MESSAGES.toString(), new JSONArray());
                    case LATEST_ALERT -> jsonObject.put(Fields.LATEST_ALERT.toString(), new JSONObject());
                    case ALERT_VIEWERS -> jsonObject.put(Fields.ALERT_VIEWERS.toString(), new JSONArray());
                }
            }

            if (changesMade) cache.updateCache(Document.parse(jsonObject.toString()));
        }
    }

    public static BotDB ins() {
        return INSTANCE;
    }

    public enum Fields implements GenericJSONField {
        LAST_BOOTED("last_booted"),
        SUGGESTIONS_OBJECT("suggestions"),
        REPORTS_OBJECT("reports"),
        DEVELOPERS_ARRAY("developers"),
        RANDOM_MESSAGES("random_messages"),
        LATEST_ALERT("latest_alert"),
        ALERT_VIEWERS("alert_viewers");

        public enum SubFields {
            SUGGESTIONS_ACCEPTED_CHANNEL("accepted_channel"),
            SUGGESTIONS_DENIED_CHANNEL("denied_channel"),
            SUGGESTIONS_PENDING_CHANNEL("pending_channel"),
            SUGGESTIONS_CATEGORY("suggestions_category"),
            SUGGESTIONS_BANNED_USERS("banned_users"),

            REPORTS_CHANNEL("reports_channel"),
            REPORTS_CATEGORY("reports_category"),
            REPORTS_BANNED_USERS("banned_users"),

            ALERT("alert"),
            ALERT_TIME("alert_time");


            private final String str;

            SubFields(String str) {
                this.str = str;
            }

            @Override
            public String toString() {
                return str;
            }
        }

        private final String str;

        Fields(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
