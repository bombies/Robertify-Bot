package main.utils.database.mongodb.databases

import main.constants.database.MongoDatabaseKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.database.mongodb.DocumentBuilderKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.json.GenericJSONFieldKt
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

class BotDBKt private constructor() :
    AbstractMongoDatabaseKt(MongoDatabaseKt.ROBERTIFY_DATABASE, MongoDatabaseKt.ROBERTIFY_BOT_INFO) {
    companion object {
        private val log = LoggerFactory.getLogger(Companion::class.java)
        private var INSTANCE: BotDBKt? = null

        fun ins(): BotDBKt {
            if (INSTANCE == null)
                INSTANCE = BotDBKt()
            return INSTANCE!!
        }

        fun update() {
            log.debug("Updating Bot Info cache")
            val cache = BotDBCacheKt.instance!!
            for (obj in cache.getCache()) {
                val jsonObject = obj as JSONObject
                var changesMade = false
                for (fields in Fields.values()) {
                    if (jsonObject.has(fields.toString())) continue
                    changesMade = true
                    when (fields) {
                        Fields.LAST_BOOTED -> jsonObject.put(
                            Fields.LAST_BOOTED.toString(),
                            System.currentTimeMillis()
                        )

                        Fields.REPORTS_OBJECT -> jsonObject.put(
                            Fields.REPORTS_OBJECT.toString(), JSONObject()
                                .put(Fields.SubFields.REPORTS_CATEGORY.toString(), -1L)
                                .put(Fields.SubFields.REPORTS_CHANNEL.toString(), -1)
                                .put(Fields.SubFields.REPORTS_BANNED_USERS.toString(), -1)
                        )

                        Fields.SUGGESTIONS_OBJECT -> jsonObject.put(
                            Fields.SUGGESTIONS_OBJECT.toString(), JSONObject()
                                .put(Fields.SubFields.SUGGESTIONS_CATEGORY.toString(), -1L)
                                .put(Fields.SubFields.SUGGESTIONS_PENDING_CHANNEL.toString(), -1L)
                                .put(Fields.SubFields.SUGGESTIONS_ACCEPTED_CHANNEL.toString(), -1L)
                                .put(Fields.SubFields.SUGGESTIONS_DENIED_CHANNEL.toString(), -1L)
                                .put(Fields.SubFields.SUGGESTIONS_BANNED_USERS.toString(), -1L)
                        )

                        Fields.DEVELOPERS_ARRAY -> jsonObject.put(
                            Fields.DEVELOPERS_ARRAY.toString(),
                            JSONArray()
                        )

                        Fields.RANDOM_MESSAGES -> jsonObject.put(
                            Fields.RANDOM_MESSAGES.toString(),
                            JSONArray()
                        )

                        Fields.LATEST_ALERT -> jsonObject.put(Fields.LATEST_ALERT.toString(), JSONObject())
                        Fields.ALERT_VIEWERS -> jsonObject.put(Fields.ALERT_VIEWERS.toString(), JSONArray())
                        else -> {}
                    }
                }
                if (changesMade) cache.updateCache(Document.parse(jsonObject.toString()))
            }
        }
    }

    override fun init() {
        if (collection.countDocuments() != 0L)
            return

        addDocument(
            DocumentBuilderKt.create()
                .addField("identifier", "robertify_main_config")
                .addField(Fields.LAST_BOOTED, 0L)
                .addField(
                    Fields.SUGGESTIONS_OBJECT, JSONObject()
                        .put(Fields.SubFields.SUGGESTIONS_CATEGORY.toString(), -1L)
                        .put(Fields.SubFields.SUGGESTIONS_PENDING_CHANNEL.toString(), -1L)
                        .put(Fields.SubFields.SUGGESTIONS_ACCEPTED_CHANNEL.toString(), -1L)
                        .put(Fields.SubFields.SUGGESTIONS_DENIED_CHANNEL.toString(), -1L)
                        .put(Fields.SubFields.SUGGESTIONS_BANNED_USERS.toString(), JSONArray())
                )
                .addField(
                    Fields.REPORTS_OBJECT, JSONObject()
                        .put(Fields.SubFields.REPORTS_CATEGORY.toString(), -1L)
                        .put(Fields.SubFields.REPORTS_CHANNEL.toString(), -1)
                        .put(Fields.SubFields.REPORTS_BANNED_USERS.toString(), JSONArray())
                )
                .addField(Fields.DEVELOPERS_ARRAY, JSONArray())
                .addField(Fields.LATEST_ALERT, JSONObject())
                .addField(Fields.ALERT_VIEWERS, JSONArray())
                .addField(Fields.GUILD_COUNT, 0)
                .build()
        )
    }


    enum class Fields(private val str: String) : GenericJSONFieldKt {
        LAST_BOOTED("last_booted"),
        SUGGESTIONS_OBJECT("suggestions"),
        REPORTS_OBJECT("reports"),
        DEVELOPERS_ARRAY("developers"),
        RANDOM_MESSAGES("random_messages"),
        LATEST_ALERT("latest_alert"),
        ALERT_VIEWERS("alert_viewers"),
        GUILD_COUNT("guild_count");

        enum class SubFields(private val str: String) {
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

            override fun toString(): String {
                return str
            }
        }

        override fun toString(): String {
            return str
        }
    }

}