package main.commands.slashcommands.commands.misc.reminders;

import lombok.SneakyThrows;
import main.commands.slashcommands.commands.misc.reminders.jobs.ReminderJob;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.quartz.JobBuilder.newJob;

public class ReminderScheduler {
    private final static Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);
    private final Guild guild;
    private final Scheduler scheduler;

    public ReminderScheduler(Guild guild) {
        this.guild = guild;
        this.scheduler = Robertify.getCronScheduler();
    }

    @SneakyThrows
    public void scheduleReminder(
            long user,
            long destination,
            int hour,
            int minute,
            String reminder,
            int reminderID,
            @Nullable String timeZone
    ) {
        if (guild == null)
            throw new NullPointerException("Why is the guild invalid??");

        final var scheduledJob = newJob(ReminderJob.class)
                .withIdentity("reminder#" + guild.getId() + "#" + user + "#" + reminderID, "reminders")
                .usingJobData("guild", guild.getIdLong())
                .usingJobData("destination", destination)
                .usingJobData("user", user)
                .usingJobData("reminder", reminder)
                .build();
        final var jobTrigger = TriggerBuilder.newTrigger()
                .withIdentity("reminder_trigger#" + guild.getId() + "#" + user + "#" + reminderID, "reminders")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule("0 %d %d * * ?".formatted(minute, hour))
                                .inTimeZone(timeZone == null ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZone))
                )
                .build();
        scheduler.scheduleJob(scheduledJob, jobTrigger);
    }

    @SneakyThrows
    public void removeReminder(long user, int reminderID) {
        scheduler.deleteJob(JobKey.jobKey("reminder#" + guild.getId() + "#" + user + "#" + reminderID, "reminders"));
    }

    public void editReminder(long channelID, long user, int reminderID, int newHour, int newMinute, String reminder, String timeZone) {
        removeReminder(user, reminderID);
        scheduleReminder(user, channelID, newHour, newMinute, reminder, reminderID, timeZone);
    }
}
