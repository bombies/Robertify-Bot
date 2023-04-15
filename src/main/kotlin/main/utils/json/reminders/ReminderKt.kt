package main.utils.json.reminders

import java.util.TimeZone

data class ReminderKt(
    val id: Int,
    val reminder: String,
    val userId: Long,
    val channelId: Long,
    private val reminderTime: Long,
    private val timezone: String?
) {
    val hour: Int = ((reminderTime / 1000) / 60 / 60 % 24).toInt()
    val minute: Int = ((reminderTime/ 1000) / 60 % 60).toInt()

    fun getTimeZone(): TimeZone =
        if (timezone == null) TimeZone.getDefault() else TimeZone.getTimeZone(timezone)

    override fun toString(): String = reminder
}