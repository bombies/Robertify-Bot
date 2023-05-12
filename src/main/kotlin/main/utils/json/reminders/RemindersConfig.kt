package main.utils.json.reminders

import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import main.utils.json.AbstractGuildConfig
import main.utils.json.GenericJSONField
import main.utils.json.reminders.scheduler.ReminderScheduler
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class RemindersConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    companion object {
        val logger by SLF4J

        val validTimeZones = listOf(
            "UTC",
            "EST",
            "CST",
            "MST",
            "PST",
            "GMT",
            "BST",
            "CET",
            "EET",
            "IST",
            "JST",
            "NZST",
            "AEST",
            "ACST",
            "AWST"
        )
    }

    init {
        if (!getGuildObject().has(Fields.REMINDERS.toString()))
            update()
    }

    fun addUser(uid: Long) {
        if (userExists(uid)) return
        val guildObject = getGuildObject()
        val userArr = guildObject.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        val userObj = JSONObject()
        userObj.put(Fields.USER_ID.toString(), uid)
        userObj.put(Fields.USER_REMINDERS.toString(), JSONArray())
        userObj.put(Fields.IS_BANNED.toString(), false)
        userArr.put(userObj)
        cache.updateGuild(guildObject)
    }

    operator fun plus(reminder: Reminder) {
        if (!userExists(reminder.userId)) addUser(reminder.userId)
        val guildObj = getGuildObject()
        val userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        val userReminders = userArr
            .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, reminder.userId))
            .getJSONArray(Fields.USER_REMINDERS.toString())
        val reminderObj = JSONObject()
            .put(Fields.REMINDER.toString(), reminder)
            .put(Fields.REMINDER_CHANNEL.toString(), reminder.channelId)
            .put(
                Fields.REMINDER_TIME.toString(),
                reminder.hour.toDuration(DurationUnit.HOURS).inWholeMilliseconds + reminder.minute.toDuration(
                    DurationUnit.MINUTES
                ).inWholeMilliseconds
            )
        reminderObj.put(Fields.REMINDER_TIMEZONE.toString(), reminder.timezone.getDisplayName(false, TimeZone.SHORT))
        userReminders.put(reminderObj)
        cache.updateGuild(guildObj)
    }

    fun addReminder(uid: Long, reminder: String, channelID: Long, reminderTime: Long, timeZone: String?) {
        if (!userExists(uid)) addUser(uid)
        val guildObj = getGuildObject()
        val userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        val userReminders = userArr
            .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
            .getJSONArray(Fields.USER_REMINDERS.toString())
        val reminderObj = JSONObject()
            .put(Fields.REMINDER.toString(), reminder)
            .put(Fields.REMINDER_CHANNEL.toString(), channelID)
            .put(Fields.REMINDER_TIME.toString(), reminderTime)
        if (timeZone != null) reminderObj.put(Fields.REMINDER_TIMEZONE.toString(), timeZone)
        userReminders.put(reminderObj)
        cache.updateGuild(guildObj)
    }

    operator fun minus(terms: Pair<Long, Int>) {
        val uid = terms.first
        val id = terms.second

        if (!userHasReminders(uid))
            throw NullPointerException("This user doesn't have any reminders in this guild!")

        val guildObj = getGuildObject()
        val userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        val userReminders = userArr
            .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
            .getJSONArray(Fields.USER_REMINDERS.toString())
        require(!(id < 0 || id > userReminders.length() - 1)) { "The ID provided is invalid!" }
        userReminders.remove(id)
        cache.updateGuild(guildObj)
    }

    fun removeReminder(uid: Long, id: Int) =
        minus(Pair(uid, id))

    operator fun minus(uid: Long) {
        if (!userHasReminders(uid) || get(uid).isNullOrEmpty())
            throw NullPointerException("This user doesn't have any reminders in this guild!")

        val guildObj = getGuildObject()
        val userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        val userReminders = userArr
            .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
            .getJSONArray(Fields.USER_REMINDERS.toString())

        val reminderScheduler = ReminderScheduler(guild)

        for (i in 0 until userReminders.length())
            reminderScheduler.removeReminder(uid, i)
        userReminders.clear()
        cache.updateGuild(guildObj)
    }

    fun clearReminders(uid: Long) =
        minus(uid)

    fun removeAllReminderChannels(uid: Long) {
        if (!userExists(uid))
            throw NullPointerException("This user doesn't have any reminders!")

        val guildObj = getGuildObject()
        val allReminders = getAllReminders(guildObj, uid)

        for (reminder in allReminders)
            (reminder as JSONObject).put(Fields.REMINDER_CHANNEL.toString(), -1L)
        cache.updateGuild(guildObj)
    }

    fun removeReminderChannel(uid: Long, id: Int) {
        editReminderChannel(uid, id, -1L)
    }

    fun editReminderChannel(uid: Long, id: Int, channelID: Long) {
        if (!userExists(uid))
            throw NullPointerException("This user doesn't have any reminders to edit!")

        val guildObj = getGuildObject()
        val reminder = getSpecificReminder(guildObj, uid, id)
        reminder.put(Fields.REMINDER_CHANNEL.toString(), channelID)

        cache.updateGuild(guildObj)
    }

    fun editReminderTime(uid: Long, id: Int, time: Long) {
        if (!userExists(uid))
            throw NullPointerException("This user doesn't have any reminders to edit!")

        val guildObj = getGuildObject()
        val reminder = getSpecificReminder(guildObj, uid, id)
        reminder.put(Fields.REMINDER_TIME.toString(), time)

        cache.updateGuild(guildObj)
    }

    fun banUser(uid: Long) {
        setBanState(uid, true)
    }

    fun unbanUser(uid: Long) {
        setBanState(uid, false)
    }

    private fun setBanState(uid: Long, state: Boolean) {
        if (state)
            check(!userIsBanned(uid)) { "This user is already banned!" }
        else check(userIsBanned(uid)) { "This user is not banned!" }

        val guildObj = getGuildObject()
        val usersArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        val userObj = usersArr.getJSONObject(getIndexOfObjectInArray(usersArr, Fields.USER_ID, uid))
        userObj.put(Fields.IS_BANNED.toString(), state)
        cache.updateGuild(guildObj)
    }

    fun userIsBanned(uid: Long): Boolean {
        if (!userExists(uid))
            addUser(uid)
        return getUser(uid)!!.isBanned
    }

    fun userHasReminders(uid: Long): Boolean {
        return if (!userExists(uid)) false else getReminders(uid) != null
    }

    operator fun get(uid: Long): List<Reminder>? =
        Collections.unmodifiableList(getUser(uid)!!.reminders)

    fun getReminders(uid: Long): List<Reminder>? =
        get(uid)


    fun getUser(uid: Long): ReminderUser? {
        val guildObject = getGuildObject()

        val reminderObj: JSONObject = try {
            guildObject.getJSONObject(Fields.REMINDERS.toString())
        } catch (e: JSONException) {
            update()
            try {
                guildObject.getJSONObject(Fields.REMINDERS.toString())
            } catch (e2: JSONException) {
                guildObject.getJSONObject(Fields.REMINDERS.toString())
            }
        }

        val users = reminderObj.getJSONArray(Fields.USERS.toString())

        return try {
            val userObj = users.getJSONObject(getIndexOfObjectInArray(users, Fields.USER_ID, uid))
            val reminders = userObj.getJSONArray(Fields.USER_REMINDERS.toString())
            val isBanned = userObj.getBoolean(Fields.IS_BANNED.toString())
            val ret: MutableList<Reminder> = ArrayList()
            for ((i, reminder) in reminders.withIndex()) {
                val actualObj = reminder as JSONObject
                ret.add(
                    Reminder(
                        i,
                        actualObj.getString(Fields.REMINDER.toString()),
                        uid,
                        actualObj.getLong(Fields.REMINDER_CHANNEL.toString()),
                        actualObj.getLong(Fields.REMINDER_TIME.toString()),
                        if (actualObj.has(Fields.REMINDER_TIMEZONE.toString())) actualObj.getString(
                            Fields.REMINDER_TIMEZONE.toString()
                        ) else null
                    )
                )
            }
            ReminderUser(uid, guild.idLong, ret, isBanned)
        } catch (e: NullPointerException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    fun getAllGuildUsers(): List<ReminderUser> {
        val guildObject = getGuildObject()

        val reminderObj: JSONObject = try {
            guildObject.getJSONObject(Fields.REMINDERS.toString())
        } catch (e: JSONException) {
            return ArrayList()
        }

        val users = reminderObj.getJSONArray(Fields.USERS.toString())

        return try {
            val reminderUsers: MutableList<ReminderUser> = ArrayList()
            for (userObj in users) {
                val actualUser = userObj as JSONObject
                val uid = actualUser.getLong(Fields.USER_ID.toString())
                val reminders = actualUser.getJSONArray(Fields.USER_REMINDERS.toString())
                val isBanned = actualUser.getBoolean(Fields.IS_BANNED.toString())
                val reminderList: MutableList<Reminder> = ArrayList()
                for ((i, reminder) in reminders.withIndex()) {
                    val actualObj = reminder as JSONObject
                    reminderList.add(
                        Reminder(
                            i,
                            actualObj.getString(Fields.REMINDER.toString()),
                            uid,
                            actualObj.getLong(Fields.REMINDER_CHANNEL.toString()),
                            actualObj.getLong(Fields.REMINDER_TIME.toString()),
                            if (actualObj.has(Fields.REMINDER_TIMEZONE.toString())) actualObj.getString(
                                Fields.REMINDER_TIMEZONE.toString()
                            ) else null
                        )
                    )
                }
                reminderUsers.add(ReminderUser(uid, guild.idLong, reminderList, isBanned))
            }
            reminderUsers
        } catch (e: NullPointerException) {
            emptyList()
        }
    }

    fun guildHasReminders(): Boolean =
        getAllGuildUsers().isNotEmpty()

    fun banChannel(cid: Long) {
        check(!channelIsBanned(cid)) { "This channel is already banned!" }
        val guildObj = getGuildObject()
        val channelsArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.BANNED_CHANNELS.toString())
        channelsArr.put(cid)
        cache.updateGuild(guildObj)
    }

    fun unbanChannel(cid: Long) {
        check(channelIsBanned(cid)) { "This channel is not banned!" }
        val guildObj = getGuildObject()
        val channelsArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.BANNED_CHANNELS.toString())
        channelsArr.remove(getIndexOfObjectInArray(channelsArr, cid))
        cache.updateGuild(guildObj)
    }

    fun channelIsBanned(cid: Long): Boolean {
        val guildObj = getGuildObject()
        return try {
            val array = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.BANNED_CHANNELS.toString())
            val list = array.toList()
            list.stream().anyMatch { obj: Any -> obj as Long == cid }
        } catch (e: JSONException) {
            update()
            false
        }
    }

    private fun userExists(uid: Long): Boolean =
        getUser(uid) != null

    private fun getSpecificReminder(uid: Long, id: Int): JSONObject =
        getSpecificReminder(getGuildObject(), uid, id)

    private fun getSpecificReminder(guildObj: JSONObject, uid: Long, id: Int): JSONObject {
        if (!userHasReminders(uid))
            throw NullPointerException("This user doesn't have any reminders in this guild!")
        val userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        val userReminders = userArr
            .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
            .getJSONArray(Fields.USER_REMINDERS.toString())
        return userReminders.getJSONObject(id)
    }

    private fun getAllReminders(guildObj: JSONObject, uid: Long): JSONArray {
        if (!userHasReminders(uid)) throw NullPointerException("This user doesn't have any reminders in this guild!")
        val userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
            .getJSONArray(Fields.USERS.toString())
        return userArr
            .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
            .getJSONArray(Fields.USER_REMINDERS.toString())
    }

    suspend fun scheduleReminders() = coroutineScope {
        launch {
            logger.debug("Attempting to schedule guild reminders for ${guild.name}")
            val scheduler = ReminderScheduler(guild)

            if (!guildHasReminders()) {
                logger.debug("${guild.name} didn't have any reminders to schedule.")
                return@launch
            }

            val allGuildUsers = getAllGuildUsers()
            allGuildUsers.forEach { user ->
                val reminders = user.reminders

                logger.debug("Attempting to schedule reminder(s) for ${user.id} in ${guild.name}")
                reminders.forEach { reminder ->
                    logger.debug(
                        """
                        Scheduling reminder with information:
                        User Id: ${user.id}
                        Channel Id: ${reminder.channelId}
                        Hour: ${reminder.hour}
                        Minute: ${reminder.minute}
                        Reminder: ${reminder.reminder}
                        Reminder Id: ${reminder.id}
                    """.trimIndent()
                    )
                    scheduler.scheduleReminder(
                        user = user.id,
                        destination = reminder.channelId,
                        hour = reminder.hour,
                        minute = reminder.minute,
                        reminder = reminder.reminder,
                        reminderId = reminder.id,
                        timeZone = reminder.timezone.id
                    )
                }
                logger.debug("Scheduled all ${reminders.size} reminder(s) for ${user.id} in ${guild.name}")
            }
        }
    }

    suspend fun unscheduleReminders() = coroutineScope {
        launch {
            val scheduler = ReminderScheduler(guild)

            if (!guildHasReminders())
                return@launch

            val allUsers = getAllGuildUsers()
            allUsers.forEach { user ->
                val reminders = user.reminders
                reminders.forEach { reminder ->
                    scheduler.removeReminder(user.id, reminder.id)
                }
            }
        }
    }

    override fun update() {
        val guildObject = getGuildObject()
        if (!guildObject.has(Fields.REMINDERS.toString())) {
            val reminderObj = JSONObject()
            reminderObj.put(Fields.USERS.toString(), JSONArray())
            reminderObj.put(Fields.BANNED_CHANNELS.toString(), JSONArray())
            guildObject.put(Fields.REMINDERS.toString(), reminderObj)
        }
        cache.updateGuild(guildObject)
    }

    enum class Fields : GenericJSONField {
        REMINDERS,
        USERS,
        USER_ID,
        USER_REMINDERS,
        REMINDER,
        REMINDER_CHANNEL,
        REMINDER_TIME,
        REMINDER_TIMEZONE,
        IS_BANNED,
        BANNED_CHANNELS;

        override fun toString(): String =
            super.name.lowercase(Locale.getDefault())
    }

}