package main.utils.database.mongodb.cache

import kotlinx.coroutines.*
import main.utils.database.mongodb.databases.GuildDBKt
import main.utils.database.mongodb.databases.GuildDBKt.getGuildDocument
import org.bson.Document
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class GuildDBCacheKt private constructor() : AbstractMongoCacheKt(GuildDBKt) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val executor = Executors.newSingleThreadScheduledExecutor()
        private val ExecutorServiceDispatcher = executor.asCoroutineDispatcher()
        private val scheduledUnloads = HashMap<Long, ScheduledFuture<*>>()
        val ins: GuildDBCacheKt by lazy { GuildDBCacheKt() }
    }
    
    init {
        init()
        logger.debug("Done instantiating the Guild cache!")
    }

    @Synchronized
    fun getField(gid: Long, field: GuildDBKt.Field): Any? {
        if (!guildHasInfo(gid)) loadGuild(gid)
        delayUnload(gid)
        return getGuildInfo(gid)!![field.toString()]
    }

    @Synchronized
    fun setField(gid: Long, field: GuildDBKt.Field, value: Any?) {
        if (!guildHasInfo(gid)) loadGuild(gid)
        val guildInfo = getGuildInfo(gid)
        guildInfo!!.put(field.toString(), value)
        updateCache(guildInfo, GuildDBKt.Field.GUILD_ID, gid)
        delayUnload(gid)
    }

    @Synchronized
    fun hasField(gid: Long, field: GuildDBKt.Field): Boolean {
        if (!guildHasInfo(gid)) loadGuild(gid)
        delayUnload(gid)
        return getGuildInfo(gid)!!.has(field.toString())
    }

    @Synchronized
    fun getGuildInfo(gid: Long): JSONObject? {
        if (!guildHasInfo(gid)) loadGuild(gid)
        delayUnload(gid)
        return try {
            getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildDBKt.Field.GUILD_ID, gid))
        } catch (e: JSONException) {
            null
        } catch (e: NullPointerException) {
            null
        } catch (e: Exception) {
            logger.error("An unexpected error occurred!", e)
            null
        }
    }

    @Synchronized
    fun guildHasInfo(gid: Long): Boolean {
        return try {
            getCache().getJSONObject(getIndexOfObjectInArray(getCache(), GuildDBKt.Field.GUILD_ID, gid))
            true
        } catch (e: JSONException) {
            false
        } catch (e: NullPointerException) {
            false
        }
    }

    @Synchronized
    fun loadGuild(gid: Long) = loadGuild(gid, 0)


    /**
     * Recursively attempt to load the guild into cache
     * @param gid The ID of the guild
     * @param attempt The recursive attempt
     */
    @Synchronized
    private fun loadGuild(gid: Long, attempt: Int) {
        var scopedAttempt = attempt
        logger.debug("Attempting to load guild with ID: {}", gid)
        try {
            val guildJSON = getDocument(GuildDBKt.Field.GUILD_ID.toString(), gid)
            if (guildJSON != null) {
                getCache().put(JSONObject(guildJSON))
                logger.debug("Loaded guild with ID: {}", gid)
                scheduleUnload(gid)
            }
        } catch (e: NullPointerException) {
            if (scopedAttempt == 2) return
            logger.debug(
                "Guild with ID {} didn't exist in the database. Attempting to add and reload.",
                gid
            )
            addDocument(getGuildDocument(gid))
            loadGuild(gid, ++scopedAttempt)
        }
    }

    fun loadAllGuilds() {
        logger.debug("Attempting to load all guilds")
        collection.find().forEach(Consumer<Document> { document: Document ->
            val jsonObject = JSONObject(document.toJson())
            getCache().put(jsonObject)
            val gid = jsonObject.getLong(GuildDBKt.Field.GUILD_ID.toString())
            logger.debug("Loaded guild with id {}", gid)
            scheduleUnload(gid)
        })
    }

    private fun scheduleUnload(gid: Long) {
        if (!scheduledUnloads.containsKey(gid)) {
            logger.debug("Scheduling unload for guild with ID: {}", gid)
            val scheduledUnload = executor.schedule({
                unloadGuild(gid)
                scheduledUnloads.remove(gid)
            }, 1, TimeUnit.HOURS)
            scheduledUnloads[gid] = scheduledUnload
        } else delayUnload(gid)
    }

    fun delayUnload(gid: Long) {
        require(scheduledUnloads.containsKey(gid)) { "There was no scheduled unload to delay for guild with ID: $gid" }
        logger.debug("Delaying unload for guild with ID: {}", gid)
        scheduledUnloads[gid]!!.cancel(false)
        scheduledUnloads.remove(gid)
        val scheduledUnload = executor.schedule({
            unloadGuild(
                gid
            )
        }, 1, TimeUnit.HOURS)
        scheduledUnloads[gid] = scheduledUnload
    }

    @Synchronized
    fun unloadGuild(gid: Long) {
        try {
            getCache().remove(getIndexOfObjectInArray(getCache(), GuildDBKt.Field.GUILD_ID, gid))
            logger.debug("Unloaded guild with ID: {}", gid)
        } catch (e: NullPointerException) {
            logger.debug("Guild with ID {} could not be unloaded since it wasn't found in the cache!", gid)
        }
    }
}