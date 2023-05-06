package main.utils.json.reminders

import java.util.TimeZone

data class ReminderKt(
    val id: Int,
    val reminder: String,
    val userId: Long,
    val channelId: Long,
    val reminderTime: Long,
    private val _timezone: String?
) {
    val hour: Int = ((reminderTime / 1000) / 60 / 60 % 24).toInt()
    val minute: Int = ((reminderTime/ 1000) / 60 % 60).toInt()
    val timezone: TimeZone = if (_timezone == null) TimeZone.getDefault() else TimeZone.getTimeZone(_timezone)

    override fun toString(): String = reminder
}