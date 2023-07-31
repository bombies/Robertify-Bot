package main.utils.json.reminders

data class ReminderUser(
    val id: Long,
    val guildID: Long,
    val reminders: List<Reminder>,
    val isBanned: Boolean
)