package main.utils.database.mongodb.cache

import main.utils.database.mongodb.databases.BotDB
import main.utils.database.mongodb.databases.BotDBKt
import main.utils.json.GenericJSONField
import net.dv8tion.jda.internal.utils.tuple.Pair
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class BotDBCacheKt private constructor() : AbstractMongoCacheKt(BotDBKt.ins()) {
    
    companion object {
        private val log = LoggerFactory.getLogger(Companion::class.java)
        var instance: BotDBCacheKt? = null
            private set
    }
    
    init {
        init()
        updateCache()
    }
    
    fun setLastStartup(time: Long) {
        val document = getDocument()
        document.put(BotDBKt.Fields.LAST_BOOTED.toString(), time)
    }
    
    fun getLastStartup(): Long = getDocument().getLong(BotDBKt.Fields.LAST_BOOTED.toString())
    
    fun initSuggestionChannels(
        categoryID: Long, 
        pendingChannel: Long,
        acceptedChannel: Long,
        deniedChannel: Long
    ) {
        check(!isSuggestionsSetup()) { "The channels have already been setup!" }

        val obj = getDocument()
        val suggestionObj = obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())

        suggestionObj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), categoryID)
        suggestionObj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), pendingChannel)
        suggestionObj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), acceptedChannel)
        suggestionObj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), deniedChannel)

        update(obj)
    }

    fun resetSuggestionsConfig() {
        val obj = getDocument()
        val suggestionObj = obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
        suggestionObj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), -1L)
        suggestionObj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), -1L)
        suggestionObj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), -1L)
        suggestionObj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), -1L)
        update(obj)
    }

    fun getSuggestionsCategoryID(): Long {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString())
    }

    fun getSuggestionsPendingChannelID(): Long {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getLong(SuggestionsConfigField.PENDING_CHANNEL.toString())
    }

    fun getSuggestionsAcceptedChannelID(): Long {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getLong(SuggestionsConfigField.ACCEPTED_CHANNEL.toString())
    }

    fun getSuggestionsDeniedChannelID(): Long {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getLong(SuggestionsConfigField.DENIED_CHANNEL.toString())
    }

    fun banSuggestionsUser(id: Long) {
        check(!userIsSuggestionBanned(id)) { "This user is already banned!" }
        val obj = getDocument()
        obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString()).put(id)
        update(obj)
    }

    fun unbanSuggestionUser(id: Long) {
        check(userIsSuggestionBanned(id)) { "This user is not banned!" }
        val obj = getDocument()
        val jsonArray = obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString())
        jsonArray.remove(getIndexOfObjectInArray(jsonArray, id))
        update(obj)
    }

    fun getBannedSuggestionUsers(): List<Long> {
        val ret: MutableList<Long> = ArrayList()
        val arr = getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString())
        for (i in 0 until arr.length()) ret.add(arr.getLong(i))
        return ret
    }

    fun userIsSuggestionBanned(id: Long): Boolean {
        val array = getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString())
        return arrayHasObject(array, id)
    }

    fun isSuggestionsSetup(): Boolean {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
            .getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString()) != -1L
    }

    fun initReportChannels(categoryID: Long, channelID: Long) {
        check(!isReportsSetup()) { "The reports category has already been setup!" }
        val obj: JSONObject = getDocument()
        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .put(ReportsConfigField.CATEGORY.toString(), categoryID)
        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .put(ReportsConfigField.CHANNEL.toString(), channelID)
        update(obj)
    }

    fun resetReportsConfig() {
        val obj: JSONObject = getDocument()
        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .put(ReportsConfigField.CATEGORY.toString(), -1L)
        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .put(ReportsConfigField.CHANNEL.toString(), -1L)
        update(obj)
    }

    fun isReportsSetup(): Boolean {
        return getDocument().getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .getLong(ReportsConfigField.CATEGORY.toString()) != -1L
    }

    fun getReportsID(field: ReportsConfigField): Long {
        return getDocument().getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .getLong(field.toString())
    }

    fun isUserReportsBanned(id: Long): Boolean {
        return arrayHasObject(
            getDocument().getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .getJSONArray(ReportsConfigField.BANNED_USERS.toString()), id
        )
    }

    fun banReportsUser(id: Long) {
        check(!isUserReportsBanned(id)) { "This user has already been banned!" }
        val obj: JSONObject = getDocument()
        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .getJSONArray(ReportsConfigField.BANNED_USERS.toString()).put(id)
        update(obj)
    }

    fun unbanReportsUser(id: Long) {
        check(isUserReportsBanned(id)) { "This user isn't banned!" }
        val obj: JSONObject = getDocument()
        val arr = obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
            .getJSONArray(ReportsConfigField.BANNED_USERS.toString())
        arr.remove(getIndexOfObjectInArray(arr, id))
        update(obj)
    }

    fun addDeveloper(developer: Long) {
        if (isDeveloper(developer)) throw NullPointerException("This user is already a developer!")
        val obj: JSONObject = getDocument()
        val devArr = obj.getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString())
        devArr.put(developer)
        update(obj)
    }

    fun removeDeveloper(developer: Long) {
        if (!isDeveloper(developer)) throw NullPointerException("This user isn't a developer!")
        val obj: JSONObject = getDocument()
        val devArr = obj.getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString())
        devArr.remove(getIndexOfObjectInArray(devArr, developer))
        update(obj)
    }

    fun isDeveloper(developer: Long): Boolean {
        return arrayHasObject(getDocument().getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString()), developer)
    }

    fun getDevelopers(): List<Long> {
        val ret: MutableList<Long> = java.util.ArrayList()
        val array: JSONArray = getDocument().getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString())
        array.forEach(Consumer { `val`: Any -> ret.add(`val` as Long) })
        return ret
    }

    fun addRandomMessage(s: String) {
        val obj: JSONObject = getDocument()
        if (!obj.has(BotDB.Fields.RANDOM_MESSAGES.toString())) obj.put(
            BotDB.Fields.RANDOM_MESSAGES.toString(),
            JSONArray()
        )
        obj.getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString()).put(s)
        update(obj)
    }

    fun getRandomMessages(): List<String> {
        val ret: MutableList<String> = java.util.ArrayList()
        val arr: JSONArray = getDocument().getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString())
        for (i in 0 until arr.length()) ret.add(arr.getString(i))
        return ret
    }

    fun removeMessage(id: Int) {
        val obj: JSONObject = getDocument()
        val arr = obj.getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString())
        if (id < 0 || id > arr.length() - 1) throw IndexOutOfBoundsException("The ID passed exceeds the bounds of the array!")
        arr.remove(id)
        update(obj)
    }

    fun clearMessages() {
        val obj: JSONObject = getDocument()
        val arr = obj.getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString())
        arr.clear()
        update(obj)
    }

    fun setLatestAlert(alert: String) {
        val obj: JSONObject = getDocument()
        val alertObj = obj.getJSONObject(BotDB.Fields.LATEST_ALERT.toString())
        alertObj.put(
            BotDB.Fields.SubFields.ALERT.toString(),
            alert.replace("\\\\n".toRegex(), "\n").replace("\\\\t".toRegex(), "\t")
        )
        alertObj.put(BotDB.Fields.SubFields.ALERT_TIME.toString(), System.currentTimeMillis())
        update(obj)
        clearAlertViewers()
    }

    fun getLatestAlert(): Pair<String, Long>? {
        val obj: JSONObject = getDocument()
        val alertObj = obj.getJSONObject(BotDB.Fields.LATEST_ALERT.toString())
        return try {
            val alert = alertObj.getString(BotDB.Fields.SubFields.ALERT.toString())
            val alertTime = alertObj.getLong(BotDB.Fields.SubFields.ALERT_TIME.toString())
            Pair.of(alert, alertTime)
        } catch (e: JSONException) {
            Pair.of("", 0L)
        }
    }

    fun addAlertViewer(id: Long) {
        if (userHasViewedAlert(id)) return
        val obj: JSONObject = getDocument()
        val viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString())
        viewerArr.put(id)
        update(obj)
    }

    fun userHasViewedAlert(id: Long): Boolean {
        val obj: JSONObject = getDocument()
        val viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString())
        return arrayHasObject(viewerArr, id)
    }

    fun getAlertViewerCount(): Int {
        val obj: JSONObject = getDocument()
        val viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString())
        return viewerArr.length()
    }

    fun getPosOfAlertViewer(id: Long): Int {
        if (!userHasViewedAlert(id)) return -1
        val obj: JSONObject = getDocument()
        val viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString())
        return getIndexOfObjectInArray(viewerArr, id) + 1
    }

    fun clearAlertViewers() {
        val obj: JSONObject = getDocument()
        val viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString())
        viewerArr.clear()
        update(obj)
    }

    fun setGuildCount(count: Int) {
        val obj: JSONObject = getDocument()
        obj.put(BotDB.Fields.GUILD_COUNT.toString(), count)
        update(obj)
    }

    private fun update(jsonObject: JSONObject) {
        updateCache(jsonObject, "identifier", "robertify_main_config")
    }

    fun initCache() {
        instance = BotDBCacheKt()
    }
    
    private fun getDocument(): JSONObject {
        val cache = getCache()?.getJSONObject(0)
        checkNotNull(cache) { "The BotDB cache has not been initialized yet!" }
        return cache
    }

    enum class ReportsConfigField(private val str: String) : GenericJSONField {
        CHANNEL("reports_channel"),
        CATEGORY("reports_category"),
        BANNED_USERS("banned_users");

        override fun toString(): String = str
    }


    enum class SuggestionsConfigField(private val str: String) : GenericJSONField {
        SUGGESTIONS_CATEGORY("suggestions_category"),
        ACCEPTED_CHANNEL("accepted_channel"),
        PENDING_CHANNEL("pending_channel"),
        DENIED_CHANNEL("denied_channel"),
        BANNED_USERS("banned_users");

        override fun toString(): String = str
    }

}