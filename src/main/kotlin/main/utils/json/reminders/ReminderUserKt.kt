package main.utils.json.reminders

data class ReminderUserKt(
    val id: Long,
    val guildID: Long,
    val reminders: List<ReminderKt>,
    val isBanned: Boolean
)