package main.utils.database.mongodb.databases

import com.mongodb.client.result.DeleteResult
import main.constants.ENV
import main.constants.RobertifyTheme
import main.constants.Toggle
import main.constants.database.MongoDatabase
import main.main.Config
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.database.mongodb.DocumentBuilder
import main.utils.json.GenericJSONField
import main.utils.json.toggles.TogglesConfig
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object GuildDB :
    AbstractMongoDatabase(MongoDatabase.ROBERTIFY_DATABASE, MongoDatabase.ROBERTIFY_GUILDS) {

    fun getGuildDocument(gid: Long): Document {
        return DocumentBuilder.create()
            .addField(Field.GUILD_ID, gid)
            .addField(Field.GUILD_PREFIX, Config[ENV.PREFIX])
            .addField(Field.ANNOUNCEMENT_CHANNEL, -1L)
            .addField(Field.BANNED_USERS_ARRAY, JSONArray())
            .addField(
                Field.REQUEST_CHANNEL_OBJECT, JSONObject()
                    .put(Field.REQUEST_CHANNEL_ID.toString(), -1L)
                    .put(Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), -1L)
            )
            .addField(
                Field.PERMISSIONS_OBJECT, JSONObject()
                    .put(Field.PERMISSIONS_DJ.toString(), JSONArray())
                    .put(Field.PERMISSIONS_ADMIN.toString(), JSONArray())
                    .put(Field.PERMISSIONS_USERS.toString(), JSONObject())
            )
            .addField(
                Field.RESTRICTED_CHANNELS_OBJECT, JSONObject()
                    .put(Field.RESTRICTED_CHANNELS_TEXT.toString(), JSONArray())
                    .put(Field.RESTRICTED_CHANNELS_VOICE.toString(), JSONArray())
            )
            .addField(Field.TOGGLES_OBJECT, TogglesConfig.getDefaultToggleObject())
            .addField(Field.EIGHT_BALL_ARRAY, JSONArray())
            .addField(Field.THEME, RobertifyTheme.GREEN.name.lowercase(Locale.getDefault()))
            .build()
    }

    fun addGuild(gid: Long) = addDocument(getGuildDocument(gid))

    fun removeGuild(gid: Long): DeleteResult = when (val doc = findSpecificDocument(Field.GUILD_ID, gid)) {
        null -> throw NullPointerException("There is no guild with the ID $gid in the database already!")
        else -> removeDocument(doc)
    }

    fun findGuild(gid: Long): Document? = findSpecificDocument(Field.GUILD_ID, gid)

    fun updateGuild(gid: Long, obj: JSONObject) {
        require(obj.has(Field.GUILD_ID.toString())) { "The JSON object must have a guild_id field!" }
        val document = findGuild(gid)
            ?: throw java.lang.NullPointerException("There was no document found with guild id: $gid")
        upsertDocument(document, Document.parse(obj.toString()))
    }

    enum class Field(private val str: String) : GenericJSONField {
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
        TOGGLES_DJ(Toggle.TogglesConfigField.DJ_TOGGLES.toString()),
        TOGGLES_LOGS(Toggle.TogglesConfigField.LOG_TOGGLES.toString()),
        EIGHT_BALL_ARRAY("eight_ball"),
        THEME("theme"),
        TWENTY_FOUR_SEVEN("twenty_four_seven_mode"),
        LOG_CHANNEL("log_channel");

        override fun toString(): String {
            return str
        }
    }

}