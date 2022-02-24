package main.utils.json.reminders;

import lombok.Getter;

public class Reminder {
    @Getter
    final int id;
    @Getter
    private final String reminder;
    @Getter
    private final long  userId, channelID, reminderTime;

    public Reminder(int id, String reminder, long userId, long channelID, long reminderTime) {
        this.id = id;
        this.reminder = reminder;
        this.userId = userId;
        this.channelID = channelID;
        this.reminderTime = reminderTime;
    }
}
