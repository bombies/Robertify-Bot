package main.utils.database.mongodb.databases

import com.mongodb.client.result.DeleteResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import main.constants.ENV
import main.constants.RobertifyTheme
import main.constants.Toggle
import main.constants.database.RobertifyMongoDatabase
import main.main.Config
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.database.mongodb.DocumentBuilder
import main.utils.database.mongodb.cache.redis.guild.GuildDatabaseModel
import main.utils.json.GenericJSONField
import main.utils.json.toggles.TogglesConfig
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object GuildDB :
    AbstractMongoDatabase(RobertifyMongoDatabase.ROBERTIFY_DATABASE, RobertifyMongoDatabase.ROBERTIFY_GUILDS) {

    fun getGuildDocument(gid: Long): Document {
        return DocumentBuilder.create()
            .setObj(GuildDatabaseModel(server_id = gid).toJsonObject())
            .build()
    }

    fun addGuild(gid: Long) = addDocument(getGuildDocument(gid))

    fun removeGuild(gid: Long): DeleteResult = when (val doc = findSpecificDocument(Field.GUILD_ID, gid)) {
        null -> throw NullPointerException("There is no guild with the ID $gid in the database already!")
        else -> removeDocument(doc)
    }

    fun findGuild(gid: Long): Document? = findSpecificDocument(Field.GUILD_ID, gid)

    fun updateGuild(newInfo: GuildDatabaseModel) {
        val document = findGuild(newInfo.server_id)
            ?: throw java.lang.NullPointerException("There was no document found with guild id: ${newInfo.server_id}")
        upsertDocument(document, Document.parse(newInfo.toJsonObject().toString()))
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