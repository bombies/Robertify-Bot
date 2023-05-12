package main.utils.database.mongodb.cache.redis

import main.constants.RobertifyPermission
import main.utils.GeneralUtils
import main.utils.GeneralUtils.isDiscordId
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
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

    @Synchronized
    fun getField(gid: Long, field: GuildDB.Field): Any? {
        if (!guildHasInfo(gid)) loadGuild(gid)
        return getGuildInfo(gid)!![field.toString()]
    }

    @Synchronized
    fun setField(gid: Long, field: GuildDB.Field, value: Any) {
        if (!guildHasInfo(gid)) loadGuild(gid)
        val guildInfo = getGuildInfo(gid)
        guildInfo!!.put(field.toString(), value)
        setex(gid, 3600, guildInfo)
        updateGuild(guildInfo)
    }

    @Synchronized
    fun setFields(gid: Long, fields: JSONObject) {
        if (!guildHasInfo(gid)) loadGuild(gid)
        val validKeys = GuildDB.Field.values().map { it.toString() }
        val guildInfo = getGuildInfo(gid)

        fields.keySet()
            .filter { key -> validKeys.contains(key) }
            .forEach { guildInfo!!.put(it, fields[it]) }

        setex(gid, 3600, guildInfo!!)
        updateGuild(guildInfo)
    }

    @Synchronized
    fun hasField(gid: Long, field: GuildDB.Field): Boolean {
        if (!guildHasInfo(gid)) loadGuild(gid)
        return getGuildInfo(gid)!!.has(field.toString())
    }

    @Synchronized
    fun getGuildInfo(gid: Long): JSONObject? {
        return getGuildInfo(gid.toString())
    }

    @Synchronized
    fun getGuildInfo(gid: String): JSONObject? {
        if (!guildHasInfo(gid)) loadGuild(gid)
        val guildInfo = get(gid) ?: return null
        val ret = JSONObject(guildInfo)
        return correctGuildObj(ret)
    }

    fun updateGuild(obj: JSONObject, gid: Long) {
        val db = getDB()
        updateCacheNoDB(gid.toString(), readyGuildObjForRedis(obj))
        db.updateGuild(gid, correctGuildObj(obj))
    }

    fun updateGuild(obj: JSONObject) {
        updateGuild(obj, GeneralUtils.getID(obj, GuildDB.Field.GUILD_ID))
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
                    rtc.toList().forEach { item: Any ->
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

    fun correctGuildObj(obj: JSONObject?): JSONObject {
        if (!obj!!.has(GuildDB.Field.GUILD_ID.toString())) return obj
        if (obj.has("_id")) if (obj["_id"] is String) obj.put("_id", ObjectId(obj.getString("_id")))
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

    @Synchronized
    fun guildHasInfo(gid: Long): Boolean {
        return get(gid) != null
    }

    @Synchronized
    fun guildHasInfo(gid: String?): Boolean {
        return get(gid!!) != null
    }

    @Synchronized
    fun loadGuild(gid: Long) {
        loadGuild(gid.toString(), 0)
    }

    @Synchronized
    fun loadGuild(gid: String) {
        loadGuild(gid, 0)
    }

    /**
     * Recursively attempt to load the guild into cache
     * @param gid The ID of the guild
     * @param attempt The recursive attempt
     */
    @Synchronized
    private fun loadGuild(gid: String, attempt: Int) {
        var scopedAttempt = attempt
        logger.debug("Attempting to load guild with ID: {}", gid)
        try {
            val guildJSON: String? = mongoDB.getDocument(GuildDB.Field.GUILD_ID.toString(), gid.toLong())
            if (guildJSON != null) {
                val guildObj = readyGuildObjForRedis(JSONObject(guildJSON))
                setex(gid, 3600, guildObj)
                logger.debug("Loaded guild with ID: {}", gid)
            }
        } catch (e: Exception) {
            when (e) {
                is NullPointerException,
                is NoSuchElementException -> {
                    if (scopedAttempt == 2) return
                    logger.debug(
                        "Guild with ID {} didn't exist in the database. Attempting to add and reload.",
                        gid
                    )
                    mongoDB.addDocument(GuildDB.getGuildDocument(gid.toLong()))
                    loadGuild(gid, ++scopedAttempt)
                }

                else -> throw e
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