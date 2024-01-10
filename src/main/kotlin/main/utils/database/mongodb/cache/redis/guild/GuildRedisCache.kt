package main.utils.database.mongodb.cache.redis.guild

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import main.constants.RobertifyPermission
import main.utils.GeneralUtils
import main.utils.GeneralUtils.isDiscordId
import main.utils.database.mongodb.cache.redis.DatabaseRedisCache
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import org.bson.BsonMaximumSizeExceededException
import org.bson.Document
import org.bson.types.ObjectId
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

class GuildRedisCache private constructor() : DatabaseRedisCache("ROBERTIFY_GUILD", GuildDB) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        val ins: GuildRedisCache by lazy { GuildRedisCache() }

        fun initCache() {
            logger.debug("Instantiating new Guild cache!")
            AbstractGuildConfig.initCache()
        }
    }

    fun updateGuild(guild: GuildDatabaseModel) {
        if (!guildHasInfo(guild.server_id)) loadGuild(guild.server_id)
        setex(guild.server_id, 3600, readyGuildObjForRedis(guild.toJsonObject()))
        getDB().updateGuild(guild)
    }

    fun updateGuild(gid: String, block: GuildDatabaseModel.() -> Unit) {
        if (!guildHasInfo(gid)) loadGuild(gid)
        val guildModel = getGuildModel(gid)!!
        block(guildModel)
        setex(gid, 3600, readyGuildObjForRedis(guildModel.toJsonObject()))
        try {
            getDB().updateGuild(guildModel)
        } catch (e: BsonMaximumSizeExceededException) {
            del(gid)
            getDB().removeGuild(gid.toLong());
            getDB().addGuild(gid.toLong())
            setex(gid, 3600, readyGuildObjForRedis(GuildDatabaseModel(server_id = gid.toLong()).toJsonObject()))
        }
    }

    fun getGuildModel(gid: String): GuildDatabaseModel? {
        if (!guildHasInfo(gid)) loadGuild(gid)
        val guildInfo = get(gid) ?: return null
        val json = correctGuildObj(JSONObject(guildInfo)).toString()
        return Json.decodeFromString(json)
    }

    private fun readyGuildObjForRedis(obj: JSONObject): JSONObject {
        if (!obj.has(GuildDB.Field.GUILD_ID.toString())) return obj
        if (obj[GuildDB.Field.GUILD_ID.toString()] is Long) obj.put(
            GuildDB.Field.GUILD_ID.toString(), obj.getLong(GuildDB.Field.GUILD_ID.toString()).toString()
        )
        if (obj.has(GuildDB.Field.LOG_CHANNEL.toString())) if (obj[GuildDB.Field.LOG_CHANNEL.toString()] is Long) obj.put(
            GuildDB.Field.LOG_CHANNEL.toString(), obj.getLong(GuildDB.Field.LOG_CHANNEL.toString()).toString()
        )

        if (obj.has(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())) {
            val restrictedChannelObj = obj.getJSONObject(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
            var rtc = restrictedChannelObj.getJSONArray(GuildDB.Field.RESTRICTED_CHANNELS_TEXT.toString())
            var rvc = restrictedChannelObj.getJSONArray(GuildDB.Field.RESTRICTED_CHANNELS_VOICE.toString())
            val handleRestrictedChannels: (arr: JSONArray) -> JSONArray = { arr ->
                val newArr = JSONArray()
                if (!arr.isEmpty && arr[0] is Long) {
                    arr.toList().forEach { item: Any ->
                        if (item.toString().isDiscordId())
                            newArr.put(item.toString())
                    }
                }
                newArr
            }

            rtc = handleRestrictedChannels(rtc)
            rvc = handleRestrictedChannels(rvc)

            obj.put(
                GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString(), JSONObject()
                    .put(GuildDB.Field.RESTRICTED_CHANNELS_TEXT.toString(), rtc)
                    .put(GuildDB.Field.RESTRICTED_CHANNELS_VOICE.toString(), rvc)
            )
        }
        val permissionsObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())

        for (code in RobertifyPermission.codes) {
            if (!permissionsObj.has(code.toString())) continue
            val codeArr = permissionsObj.getJSONArray(code.toString())
            if (codeArr.isEmpty) continue
            if (codeArr[0] !is Long) continue
            val newArr = JSONArray()
            codeArr.forEach { item: Any -> newArr.put(item.toString()) }
            permissionsObj.put(code.toString(), newArr)
        }

        if (permissionsObj.has("users")) {
            val usersObj = permissionsObj.getJSONObject("users")
            for (user in usersObj.keySet()) {
                val userPermsArr = usersObj.getJSONArray(user)
                if (userPermsArr.isEmpty) continue
                if (userPermsArr[0] !is Long) continue
                val newArr = JSONArray()
                userPermsArr.forEach { item: Any -> newArr.put(item.toString()) }
                usersObj.put(user, newArr)
            }
        }
        val dedicatedChannelObj = obj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
        if (dedicatedChannelObj[GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString()] is Long) dedicatedChannelObj.put(
            GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(),
            dedicatedChannelObj.getLong(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString()).toString()
        )
        if (dedicatedChannelObj[GuildDB.Field.REQUEST_CHANNEL_ID.toString()] is Long) dedicatedChannelObj.put(
            GuildDB.Field.REQUEST_CHANNEL_ID.toString(),
            dedicatedChannelObj.getLong(GuildDB.Field.REQUEST_CHANNEL_ID.toString()).toString()
        )
        if (obj.has(GuildDB.Field.BANNED_USERS_ARRAY.toString())) {
            val bannedUserArr = obj.getJSONArray(GuildDB.Field.BANNED_USERS_ARRAY.toString())
            for (i in 0 until bannedUserArr.length()) {
                val bannedUserObj = bannedUserArr.getJSONObject(i)
                if (bannedUserObj[GuildDB.Field.BANNED_AT.toString()] is Long) bannedUserObj.put(
                    GuildDB.Field.BANNED_AT.toString(),
                    bannedUserObj.getLong(GuildDB.Field.BANNED_AT.toString()).toString()
                )
                if (bannedUserObj[GuildDB.Field.BANNED_USER.toString()] is Long) bannedUserObj.put(
                    GuildDB.Field.BANNED_USER.toString(),
                    bannedUserObj.getLong(GuildDB.Field.BANNED_USER.toString()).toString()
                )
                if (bannedUserObj[GuildDB.Field.BANNED_UNTIL.toString()] is Long) bannedUserObj.put(
                    GuildDB.Field.BANNED_UNTIL.toString(),
                    bannedUserObj.getLong(GuildDB.Field.BANNED_UNTIL.toString()).toString()
                )
                if (bannedUserObj[GuildDB.Field.BANNED_BY.toString()] is Long) bannedUserObj.put(
                    GuildDB.Field.BANNED_BY.toString(),
                    bannedUserObj.getLong(GuildDB.Field.BANNED_BY.toString()).toString()
                )
            }
        }
        return obj
    }

    private fun correctGuildObj(obj: JSONObject?): JSONObject {
        if (!obj!!.has(GuildDB.Field.GUILD_ID.toString())) return obj
        if (obj.has("_id")) obj.remove("_id")
        if (obj.has("__v")) obj.remove("__v")
        if (obj[GuildDB.Field.GUILD_ID.toString()] is String) obj.put(
            GuildDB.Field.GUILD_ID.toString(),
            obj.getString(GuildDB.Field.GUILD_ID.toString()).toLong()
        )
        if (obj.has(GuildDB.Field.LOG_CHANNEL.toString())) if (obj[GuildDB.Field.LOG_CHANNEL.toString()] is String) obj.put(
            GuildDB.Field.LOG_CHANNEL.toString(),
            obj.getString(GuildDB.Field.LOG_CHANNEL.toString()).toLong()
        )
        if (obj.has(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())) {
            val restrictedChannelObj = obj.getJSONObject(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
            var rtc = restrictedChannelObj.getJSONArray(GuildDB.Field.RESTRICTED_CHANNELS_TEXT.toString())
            var rvc = restrictedChannelObj.getJSONArray(GuildDB.Field.RESTRICTED_CHANNELS_VOICE.toString())
            val handleRestrictedChannels: (arr: JSONArray) -> JSONArray = { arr ->
                val newArr = JSONArray()
                if (!arr.isEmpty && arr[0] is String)
                    arr.toList().forEach { item: Any -> newArr.put((item as String).toLong()) }
                newArr
            }

            rtc = handleRestrictedChannels(rtc)
            rvc = handleRestrictedChannels(rvc)

            obj.put(
                GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString(), JSONObject()
                    .put(GuildDB.Field.RESTRICTED_CHANNELS_TEXT.toString(), rtc)
                    .put(GuildDB.Field.RESTRICTED_CHANNELS_VOICE.toString(), rvc)
            )
        }
        val permissionsObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
        for (code in RobertifyPermission.codes) {
            if (!permissionsObj.has(code.toString())) continue
            val codeArr = permissionsObj.getJSONArray(code.toString())
            if (codeArr.isEmpty) continue
            if (codeArr[0] !is String) continue
            val newArr = JSONArray()
            codeArr.toList().forEach { item: Any -> newArr.put((item as String).toLong()) }
            permissionsObj.put(code.toString(), newArr)
        }
        if (permissionsObj.has("users")) {
            val usersObj = permissionsObj.getJSONObject("users")
            for (user in usersObj.keySet()) {
                val userPermsArr = usersObj.getJSONArray(user)
                if (userPermsArr.isEmpty) continue
                if (userPermsArr[0] !is String) continue
                val newArr = JSONArray()
                userPermsArr.forEach { item: Any -> newArr.put((item as String).toInt()) }
                usersObj.put(user, newArr)
            }
        }
        val dedicatedChannelObj = obj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
        if (dedicatedChannelObj[GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString()] is String) dedicatedChannelObj.put(
            GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(),
            dedicatedChannelObj.getString(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString()).toLong()
        )
        if (dedicatedChannelObj[GuildDB.Field.REQUEST_CHANNEL_ID.toString()] is String) dedicatedChannelObj.put(
            GuildDB.Field.REQUEST_CHANNEL_ID.toString(),
            dedicatedChannelObj.getString(GuildDB.Field.REQUEST_CHANNEL_ID.toString()).toLong()
        )
        if (obj.has(GuildDB.Field.BANNED_USERS_ARRAY.toString())) {
            val bannedUserArr = obj.getJSONArray(GuildDB.Field.BANNED_USERS_ARRAY.toString())
            for (i in 0 until bannedUserArr.length()) {
                val bannedUserObj = bannedUserArr.getJSONObject(i)
                if (bannedUserObj[GuildDB.Field.BANNED_AT.toString()] is String) bannedUserObj.put(
                    GuildDB.Field.BANNED_AT.toString(),
                    bannedUserObj.getString(GuildDB.Field.BANNED_AT.toString()).toLong()
                )
                if (bannedUserObj[GuildDB.Field.BANNED_USER.toString()] is String) bannedUserObj.put(
                    GuildDB.Field.BANNED_USER.toString(),
                    bannedUserObj.getString(GuildDB.Field.BANNED_USER.toString()).toLong()
                )
                if (bannedUserObj[GuildDB.Field.BANNED_UNTIL.toString()] is String) bannedUserObj.put(
                    GuildDB.Field.BANNED_UNTIL.toString(),
                    bannedUserObj.getString(GuildDB.Field.BANNED_UNTIL.toString()).toLong()
                )
                if (bannedUserObj[GuildDB.Field.BANNED_BY.toString()] is String) bannedUserObj.put(
                    GuildDB.Field.BANNED_BY.toString(),
                    bannedUserObj.getString(GuildDB.Field.BANNED_BY.toString()).toLong()
                )
            }
        }
        return obj
    }

    fun guildHasInfo(gid: Long): Boolean {
        return get(gid) != null
    }

    fun guildHasInfo(gid: String?): Boolean {
        return get(gid!!) != null
    }

    fun loadGuild(gid: Long) {
        loadGuild(gid.toString(), 0)
    }

    fun loadGuild(gid: String) {
        loadGuild(gid, 0)
    }

    /**
     * Recursively attempt to load the guild into cache
     * @param gid The ID of the guild
     * @param attempt The recursive attempt
     */
    private fun loadGuild(gid: String, attempt: Int) {
        var scopedAttempt = attempt
        logger.info("Attempting to load guild with ID: {}", gid)
        try {
            val guildJSON: String? = mongoDB.getDocument(GuildDB.Field.GUILD_ID.toString(), gid.toLong())
            if (guildJSON != null) {
                val guildObj = readyGuildObjForRedis(JSONObject(guildJSON))
                setex(gid, 3600, guildObj)
                logger.info("Loaded guild with ID: {}", gid)
            }
        } catch (e: Exception) {
            when (e) {
                is NullPointerException,
                is NoSuchElementException -> {
                    if (scopedAttempt == 2) return
                    logger.info(
                        "Guild with ID {} didn't exist in the database. Attempting to add and reload.",
                        gid
                    )
                    mongoDB.addDocument(GuildDB.getGuildDocument(gid.toLong()))
                    loadGuild(gid, ++scopedAttempt)
                }

                else -> {
                    logger.error("There was an error loading guild with ID {}", gid, e)
                }
            }
        }
    }

    fun unloadGuild(gid: Long) {
        del(gid)
    }

    fun loadAllGuilds() {
        logger.debug("Attempting to load all guilds")
        collection.find().forEach { document: Document ->
            val jsonObject = readyGuildObjForRedis(JSONObject(document.toJson()))
            val gid = jsonObject.getLong(GuildDB.Field.GUILD_ID.toString())
            setex(gid, 3600, jsonObject)
            logger.debug("Loaded guild with id {}", gid)
        }
    }

    private fun getDB(): GuildDB {
        return mongoDB as GuildDB
    }

}