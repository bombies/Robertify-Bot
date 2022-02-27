package main.utils.json.reminders;

import lombok.Getter;

import java.util.List;

public class ReminderUser {
    @Getter
    private final long id;
    @Getter
    private final long guildID;
    @Getter
    private final List<Reminder> reminders;
    @Getter
    private final boolean isBanned;

    ReminderUser(long id, long guildID, List<Reminder> reminders, boolean isBanned) {
        this.id = id;
        this.guildID = guildID;
        this.reminders = reminders;
        this.isBanned = isBanned;
    }

    public boolean hasReminders() {
        return reminders != null;
    }
}
