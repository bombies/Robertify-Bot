package main.utils.database.mongodb.databases;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import main.constants.Database;
import main.constants.ENV;
import main.constants.RobertifyTheme;
import main.constants.Toggles;
import main.main.Config;
import main.main.Robertify;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.DocumentBuilder;
import main.utils.json.GenericJSONField;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GuildsDB extends AbstractMongoDatabase {
    private final static Logger logger = LoggerFactory.getLogger(GuildsDB.class);
    private static GuildsDB INSTANCE;

    private GuildsDB() {
        super(Database.MONGO.ROBERTIFY_DATABASE, Database.MONGO.ROBERTIFY_GUILDS);
    }

    @Override
    public synchronized void init() {
        final List<Guild> guilds = Robertify.shardManager.getGuilds();
        for (var guild : guilds) {
            if (specificDocumentExists(Field.GUILD_ID, guild.getIdLong()))
                continue;

            addDocument(getGuildDocument(guild.getIdLong()));
        }

        final MongoCollection<Document> collection = getCollection();
        final MongoCursor<Document> cursor = collection.find().cursor();
        final List<Document> documentsToRemove = new ArrayList<>();

        while (cursor.hasNext()) {
            Document guildDoc = cursor.next();
            JSONObject jsonObject = new JSONObject(guildDoc.toJson());
            long guildID = jsonObject.getLong(Field.GUILD_ID.toString());

            Guild filteredResult = guilds.stream()
                    .filter(guild -> guild.getIdLong() == guildID)
                    .findFirst()
                    .orElse(null);

            if (filteredResult == null)
                documentsToRemove.add(guildDoc);

        }

        if (!documentsToRemove.isEmpty())
            removeManyDocuments(documentsToRemove);
    }

    public synchronized void addGuild(long gid) {
        addDocument(getGuildDocument(gid));
    }

    public synchronized void removeGuild(long gid) {
        removeDocument(findSpecificDocument(Field.GUILD_ID, gid));
    }

    public static synchronized GuildsDB ins() {
        if (INSTANCE == null)
            INSTANCE = new GuildsDB();
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
                .addField(Field.THEME, RobertifyTheme.GREEN.name().toLowerCase())
                .build();
    }

    public static synchronized void update() {
        logger.debug("Updating Guild cache");
//        new TogglesConfig().update();
//        new PermissionsConfig().update();
//        new ThemesConfig().update();
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
            TOGGLES_DJ(Toggles.TogglesConfigField.DJ_TOGGLES.toString()),
            TOGGLES_LOGS(Toggles.TogglesConfigField.LOG_TOGGLES.toString()),
        EIGHT_BALL_ARRAY("eight_ball"),
        THEME("theme"),
        TWENTY_FOUR_SEVEN("twenty_four_seven_mode");

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
