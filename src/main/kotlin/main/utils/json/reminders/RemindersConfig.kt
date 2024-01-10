package main.utils.json.reminders

import dev.minn.jda.ktx.util.SLF4J
import kotlinx.serialization.json.Json
import main.utils.database.mongodb.cache.redis.guild.ReminderModel
import main.utils.database.mongodb.cache.redis.guild.ReminderUserModel
import main.utils.json.AbstractGuildConfig
import main.utils.json.GenericJSONField
import main.utils.json.reminders.scheduler.ReminderScheduler
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

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

    fun addUser(uid: Long) {
        if (userExists(uid)) return
        cache.updateGuild(guild.id) {
            reminders {
                users.add(
                    ReminderUserModel(
                        uid,
                        false
                    )
                )
            }
        }
    }

    fun addReminder(uid: Long, reminder: String, channelID: Long, reminderTime: Long, timeZone: String?) {
        if (!userExists(uid)) addUser(uid)

        cache.updateGuild(guild.id) {
            reminders {
                users.find { user -> user.user_id == uid }!!
                    .user_reminders.add(
                        ReminderModel(
                            reminderTime,
                            reminder,
                            timeZone,
                            channelID
                        )
                    )
            }
        }
    }

    fun removeReminder(uid: Long, id: Int) {
        if (!userHasReminders(uid))
            throw NullPointerException("This user doesn't have any reminders in this guild!")

        cache.updateGuild(guild.id) {
            reminders {
                val user = users.find { user -> user.user_id == uid }!!
                require(!(id < 0 || id > user.user_reminders.size - 1)) { "The ID provided is invalid!" }

                user.user_reminders.removeAt(id)
            }
        }
    }

    fun clearReminders(uid: Long) {
        if (!userHasReminders(uid) || getReminders(uid).isNullOrEmpty())
            throw NullPointerException("This user doesn't have any reminders in this guild!")

        cache.updateGuild(guild.id) {
            reminders {
                val scheduler = ReminderScheduler(guild)
                val reminders = users.find { user -> user.user_id == uid }!!
                    .user_reminders

                reminders.forEachIndexed { index, _ ->
                    scheduler.removeReminder(uid, index)
                }
                reminders.clear()
            }
        }
    }

    fun editReminderChannel(uid: Long, id: Int, channelID: Long) {
        if (!userExists(uid))
            throw NullPointerException("This user doesn't have any reminders to edit!")

        cache.updateGuild(guild.id) {
            reminders {
                users.find { user -> user.user_id == uid }!!
                    .user_reminders[id].reminder_channel = channelID
            }
        }
    }

    fun editReminderTime(uid: Long, id: Int, time: Long) {
        if (!userExists(uid))
            throw NullPointerException("This user doesn't have any reminders to edit!")

        cache.updateGuild(guild.id) {
            reminders {
                users.find { user -> user.user_id == uid }!!
                    .user_reminders[id].reminder_time = time
            }
        }
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

        cache.updateGuild(guild.id) {
            reminders {
                users.find { user -> user.user_id == uid }!!
                    .is_banned = state
            }
        }
    }

    fun userIsBanned(uid: Long): Boolean {
        if (!userExists(uid))
            addUser(uid)
        return getUser(uid)!!.isBanned
    }

    fun userHasReminders(uid: Long): Boolean {
        return if (!userExists(uid)) false else getReminders(uid) != null
    }

    fun getReminders(uid: Long): List<Reminder>? =
        Collections.unmodifiableList(getUser(uid)!!.reminders)


    fun getUser(uid: Long): ReminderUser? {
        val remindersObj = getGuildModel().reminders

        val user = remindersObj.users.find { user -> user.user_id == uid }
        val reminders = user?.user_reminders?.mapIndexed { i, reminder ->
            Reminder(
                i,
                reminder.reminder,
                uid,
                reminder.reminder_channel,
                reminder.reminder_time,
                reminder.reminder_timezone
            )
        }

        return if (reminders != null)
            ReminderUser(
                uid,
                guild.idLong,
                reminders,
                user.is_banned
            )
        else null
    }

    private fun getAllGuildUsers(): List<ReminderUser> {
        val users = getGuildModel().reminders.users
        return users.map { user ->
            val reminders = user.user_reminders.mapIndexed { i, reminder ->
                Reminder(
                    i,
                    reminder.reminder,
                    user.user_id,
                    reminder.reminder_channel,
                    reminder.reminder_time,
                    reminder.reminder_timezone
                )
            }
            ReminderUser(
                user.user_id,
                guild.idLong,
                reminders,
                user.is_banned
            )
        }
    }

    private fun guildHasReminders(): Boolean =
        getAllGuildUsers().any { user -> user.reminders.isNotEmpty() }

    fun banChannel(cid: Long) {
        check(!channelIsBanned(cid)) { "This channel is already banned!" }
        cache.updateGuild(guild.id) {
            reminders {
                banned_channels.add(cid)
            }
        }
    }

    fun unbanChannel(cid: Long) {
        check(channelIsBanned(cid)) { "This channel is not banned!" }
        cache.updateGuild(guild.id) {
            reminders {
                banned_channels.remove(cid)
            }
        }
    }

    fun channelIsBanned(cid: Long): Boolean {
        val reminders = getGuildModel().reminders
        return reminders.banned_channels.contains(cid)
    }

    private fun userExists(uid: Long): Boolean =
        getUser(uid) != null

    fun scheduleReminders() {
        logger.debug("Attempting to schedule guild reminders for ${guild.name}")
        val scheduler = ReminderScheduler(guild)

        if (!guildHasReminders()) {
            logger.debug("${guild.name} didn't have any reminders to schedule.")
            return
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

    fun unscheduleReminders() {
        val scheduler = ReminderScheduler(guild)

        if (!guildHasReminders())
            return

        val allUsers = getAllGuildUsers()
        allUsers.forEach { user ->
            val reminders = user.reminders
            reminders.forEach { reminder ->
                scheduler.removeReminder(user.id, reminder.id)
            }
        }
    }

    override fun update() {
        val guildObject = getGuildModel().toJsonObject()
        if (!guildObject.has(Fields.REMINDERS.toString())) {
            val reminderObj = JSONObject()
            reminderObj.put(Fields.USERS.toString(), JSONArray())
            reminderObj.put(Fields.BANNED_CHANNELS.toString(), JSONArray())
            guildObject.put(Fields.REMINDERS.toString(), reminderObj)
        }
        cache.updateGuild(Json.decodeFromString(guildObject.toString()))
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