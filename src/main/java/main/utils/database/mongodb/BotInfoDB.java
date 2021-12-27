package main.utils.database.mongodb;

import main.constants.Database;
import main.utils.json.GenericJSONField;
import org.json.JSONArray;
import org.json.JSONObject;

public class BotInfoDB extends AbstractMongoDatabase {
    private final static BotInfoDB INSTANCE = new BotInfoDB();

    private BotInfoDB() {
        super(Database.MONGO.ROBERTIFY_DATABASE, Database.MONGO.ROBERTIFY_BOT_INFO);
    }

    @Override
    public void init() {
        if (getCollection().countDocuments() == 0) {
            addDocument(
                    DocumentBuilder.create()
                            .addField("identifier", "robertify_main_config")
                            .addField(Fields.LAST_BOOTED, 0L)
                            .addField(Fields.SUGGESTIONS_OBJECT, new JSONObject()
                                    .put(Fields.SUGGESTIONS_CATEGORY.toString(), -1L)
                                    .put(Fields.SUGGESTIONS_PENDING_CHANNEL.toString(), -1L)
                                    .put(Fields.SUGGESTIONS_ACCEPTED_CHANNEL.toString(), -1L)
                                    .put(Fields.SUGGESTIONS_DENIED_CHANNEL.toString(), -1L)
                                    .put(Fields.SUGGESTIONS_BANNED_USERS.toString(), -1L)
                            )
                            .addField(Fields.REPORTS_OBJECT, new JSONObject()
                                    .put(Fields.REPORTS_CATEGORY.toString(), -1L)
                                    .put(Fields.REPORTS_CHANNEL.toString(), -1)
                                    .put(Fields.REPORTS_BANNED_USERS.toString(), -1)
                            )
                            .build()
            );
        }
    }

    public static BotInfoDB ins() {
        return INSTANCE;
    }

    public enum Fields implements GenericJSONField {
        LAST_BOOTED("last_booted"),
        SUGGESTIONS_OBJECT("suggestions"),
            SUGGESTIONS_ACCEPTED_CHANNEL("accepted_channel"),
            SUGGESTIONS_DENIED_CHANNEL("denied_channel"),
            SUGGESTIONS_PENDING_CHANNEL("pending_channel"),
            SUGGESTIONS_CATEGORY("suggestions_category"),
            SUGGESTIONS_BANNED_USERS("banned_users"),
        REPORTS_OBJECT("reports"),
            REPORTS_CHANNEL("reports_channel"),
            REPORTS_CATEGORY("reports_category"),
            REPORTS_BANNED_USERS("banned_users");

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
