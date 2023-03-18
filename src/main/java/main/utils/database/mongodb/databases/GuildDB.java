package main.utils.database.mongodb.databases;

import main.constants.Database;
import main.constants.ENV;
import main.constants.RobertifyTheme;
import main.constants.Toggles;
import main.main.Config;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.DocumentBuilder;
import main.utils.json.GenericJSONField;
import main.utils.json.toggles.TogglesConfig;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class GuildDB extends AbstractMongoDatabase {
    private final static Logger logger = LoggerFactory.getLogger(GuildDB.class);
    private static GuildDB INSTANCE;

    private GuildDB() {
        super(Database.Mongo.ROBERTIFY_DATABASE, Database.Mongo.ROBERTIFY_GUILDS);
    }

    @Override
    public synchronized void init() {

    }

    public synchronized void addGuild(long gid) {
        addDocument(getGuildDocument(gid));
    }

    public synchronized void removeGuild(long gid) {
        removeDocument(findSpecificDocument(Field.GUILD_ID, gid));
    }

    public synchronized Document findGuild(long gid) {
        try {
            return getCollection().find(in(Field.GUILD_ID.toString(), gid, Long.toString(gid))).iterator().next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public synchronized Document findGuild(String gid) {
        return findGuild(Long.parseLong(gid));
    }

    public synchronized void updateGuild(long gid, JSONObject obj) {
        if (!obj.has(GuildDB.Field.GUILD_ID.toString()))
            throw new IllegalArgumentException("The JSON object must have a guild_id field!");

        Document document = findGuild(gid);
        if (document == null)
            throw new NullPointerException("There was no document found with guild id: " + gid);

        upsertDocument(document, Document.parse(obj.toString()));
    }

    public static synchronized GuildDB ins() {
        if (INSTANCE == null)
            INSTANCE = new GuildDB();
        return INSTANCE;
    }

    public static Document getGuildDocument(long gid) {
        return DocumentBuilder.create()
                .addField(Field.GUILD_ID, gid)
                .addField(Field.GUILD_PREFIX, Config.get(ENV.PREFIX))
                .addField(Field.ANNOUNCEMENT_CHANNEL, -1L)
                .addField(Field.BANNED_USERS_ARRAY, new JSONArray())
                .addField(Field.REQUEST_CHANNEL_OBJECT, new JSONObject()
                        .put(Field.REQUEST_CHANNEL_ID.toString(), -1L)
                        .put(Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), -1L)
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
                .addField(Field.TOGGLES_OBJECT, TogglesConfig.getDefaultToggleObject())
                .addField(Field.EIGHT_BALL_ARRAY, new JSONArray())
                .addField(Field.THEME, RobertifyTheme.GREEN.name().toLowerCase())
                .build();
    }

    public static synchronized void update() {
        logger.debug("Updating Guild cache");
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
        REQUEST_CHANNEL_OBJECT("dedicated_channel"),
            REQUEST_CHANNEL_ID("channel_id"),
            REQUEST_CHANNEL_MESSAGE_ID("message_id"),
            REQUEST_CHANNEL_CONFIG("config"),
        PERMISSIONS_OBJECT("permissions"),
            PERMISSIONS_DJ("0"),
            PERMISSIONS_ADMIN("1"),
            PERMISSIONS_USERS("users"),
        RESTRICTED_CHANNELS_OBJECT("restricted_channels"),
            RESTRICTED_CHANNELS_VOICE("voice_channels"),
            RESTRICTED_CHANNELS_TEXT("text_channels"),
        TOGGLES_OBJECT("toggles"),
            TOGGLES_DJ(Toggles.TogglesConfigField.DJ_TOGGLES.toString()),
            TOGGLES_LOGS(Toggles.TogglesConfigField.LOG_TOGGLES.toString()),
        EIGHT_BALL_ARRAY("eight_ball"),
        THEME("theme"),
        TWENTY_FOUR_SEVEN("twenty_four_seven_mode"),
        LOG_CHANNEL("log_channel");

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
