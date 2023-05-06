package main.utils.json.reminders.scheduler

import main.main.RobertifyKt
import net.dv8tion.jda.api.entities.Guild
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey
import org.quartz.TriggerBuilder
import java.util.*

class ReminderSchedulerKt(private val guild: Guild) {
    private val scheduler = RobertifyKt.cronScheduler

    fun scheduleReminder(
        user: Long,
        destination: Long,
        hour: Int,
        minute: Int,
        reminder: String,
        reminderId: Int,
        timeZone: String?
    ) {
        val schedulerJob = newJob(ReminderJobKt::class.java)
            .withIdentity("reminder${guild.id}#${user}#${reminderId}", "reminders")
            .usingJobData("guild", guild.idLong)
            .usingJobData("destination", destination)
            .usingJobData("user", user)
            .usingJobData("reminder", reminder)
            .build()

        val jobTrigger = TriggerBuilder.newTrigger()
            .withIdentity("reminder_trigger#${guild.id}#${user}#${reminderId}", "reminders")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 $minute $hour * * ?")
                    .inTimeZone(if (timeZone != null) TimeZone.getTimeZone(timeZone) else TimeZone.getDefault())
            )
            .build()
        scheduler.scheduleJob(schedulerJob, jobTrigger)
    }

    fun removeReminder(user: Long, reminderId: Int): Boolean =
        scheduler.deleteJob(JobKey.jobKey("reminder${guild.id}#${user}#${reminderId}", "reminders"))

    fun editReminder(
        channelId: Long,
        user: Long,
        reminderId: Int,
        newHour: Int,
        newMinute: Int,
        reminder: String,
        timeZone: String
    ) {
        removeReminder(user, reminderId)
        scheduleReminder(user, channelId, newHour, newMinute, reminder, reminderId, timeZone)
    }
}