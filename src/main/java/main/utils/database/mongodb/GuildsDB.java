package main.utils.database.mongodb;

import main.constants.Database;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import main.utils.json.GenericJSONField;
import main.utils.json.permissions.PermissionsConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildsDB extends AbstractMongoDatabase {
    private final static Logger logger = LoggerFactory.getLogger(GuildsDB.class);
    private final static GuildsDB INSTANCE = new GuildsDB();

    private GuildsDB() {
        super(Database.MONGO.ROBERTIFY_DATABASE, Database.MONGO.ROBERTIFY_GUILDS);
    }

    @Override
    public synchronized void init() {
        for (var guild : Robertify.api.getGuilds()) {
            if (documentExists(Field.GUILD_ID, guild.getIdLong()))
                continue;

            addDocument(getGuildDocument(guild.getIdLong()));
        }
    }

    public static synchronized GuildsDB ins() {
        return INSTANCE;
    }

    public static Document getGuildDocument(long gid) {
        return DocumentBuilder.create()
                .addField(Field.GUILD_ID, gid)
                .addField(Field.GUILD_PREFIX, Config.get(ENV.PREFIX))
                .addField(Field.ANNOUNCEMENT_CHANNEL, -1L)
                .addField(Field.BANNED_USERS_ARRAY, new JSONArray())
                .addField(Field.DEDICATED_CHANNEL_OBJECT, new JSONObject()
                        .put(Field.DEDICATED_CHANNEL_ID.toString(), -1L)
                        .put(Field.DEDICATED_CHANNEL_MESSAGE_ID.toString(), -1L)
                )
                .addField(Field.PERMISSIONS_OBJECT, new JSONObject()
                        .put(Field.PERMISSIONS_DJ.toString(), new JSONArray())
                        .put(Field.PERMISSIONS_ADMIN.toString(), new JSONArray())
                        .put(Field.PERMISSIONS_USERS.toString(), new JSONObject())
                )
                .addField(Field.RESTRICTED_CHANNELS_OBJECT, new JSONObject()
                        .put(Field.RESTRICTED_CHANNELS_TEXT.toString(), new JSONArray())
                        .put(Field.RESTRICTED_CHANNELS_VOICE.toString(), new JSONArray())
                )
                .addField(Field.TOGGLES_OBJECT, new TogglesConfig().getDefaultToggleObject())
                .addField(Field.EIGHT_BALL_ARRAY, new JSONArray())
                .build();
    }

    protected static synchronized void update() {
        logger.debug("Updating Guild cache");
        new TogglesConfig().update();
        new PermissionsConfig().update();
        new ThemesConfig().update();
    }

    public enum Field implements GenericJSONField {
        GUILD_ID("server_id"),
        GUILD_PREFIX("prefix"),
        ANNOUNCEMENT_CHANNEL("announcement_channel"),
        BANNED_USERS_ARRAY("banned_users"),
            BANNED_USER("banned_id"),
            BANNED_BY("banned_by"),
            BANNED_UNTIL("banned_until"),
            BANNED_AT("banned_at"),
        DEDICATED_CHANNEL_OBJECT("dedicated_channel"),
            DEDICATED_CHANNEL_ID("channel_id"),
            DEDICATED_CHANNEL_MESSAGE_ID("message_id"),
        PERMISSIONS_OBJECT("permissions"),
            PERMISSIONS_DJ("0"),
            PERMISSIONS_ADMIN("1"),
            PERMISSIONS_USERS("users"),
        RESTRICTED_CHANNELS_OBJECT("restricted_channels"),
            RESTRICTED_CHANNELS_VOICE("voice_channels"),
            RESTRICTED_CHANNELS_TEXT("text_channels"),
        TOGGLES_OBJECT("toggles"),
            TOGGLES_DJ("dj_toggles"),
        EIGHT_BALL_ARRAY("eight_ball");

        private final String str;

        Field(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
