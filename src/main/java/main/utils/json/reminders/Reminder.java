package main.utils.json.reminders;

import lombok.Getter;

import javax.annotation.Nullable;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Reminder {
    @Getter
    final int id;
    @Getter
    private final String reminder;
    @Getter
    private final long  userId, channelID, reminderTime;
    @Getter
    private final int hour, minute;
    @Nullable
    private final String timezone;

    Reminder(int id, String reminder, long userId, long channelID, long reminderTime, @Nullable String timezone) {
        this.id = id;
        this.reminder = reminder;
        this.userId = userId;
        this.channelID = channelID;
        this.reminderTime = reminderTime;
        this.hour = (int) ((reminderTime / 1000) / 60 / 60 % 24);
        this.minute = (int) ((reminderTime/ 1000) / 60 % 60);
        this.timezone = timezone;
    }

    public TimeZone getTimeZone() {
        return timezone == null ? TimeZone.getDefault() : TimeZone.getTimeZone(timezone);
    }

    @Override
    public String toString() {
        return reminder;
    }
}
