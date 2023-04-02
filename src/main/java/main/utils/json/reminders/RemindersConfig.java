package main.utils.json.reminders;

import lombok.SneakyThrows;
import lombok.val;
import main.commands.slashcommands.commands.misc.reminders.ReminderScheduler;
import main.main.Robertify;
import main.utils.json.AbstractGuildConfig;
import main.utils.json.GenericJSONField;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemindersConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;

    public RemindersConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
        if (!getGuildObject().has(Fields.REMINDERS.toString()))
            update();
    }

    public void addUser(long uid) {
        if (userExists(uid))
            return;

        JSONObject guildObject = getGuildObject();
        final var userArr = guildObject.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());

        final var userObj = new JSONObject();
        userObj.put(Fields.USER_ID.toString(), uid);
        userObj.put(Fields.USER_REMINDERS.toString(), new JSONArray());
        userObj.put(Fields.IS_BANNED.toString(), false);

        userArr.put(userObj);

        getCache().updateGuild(guildObject);
    }

    public void addReminder(long uid, String reminder, long channelID, long reminderTime, @Nullable String timeZone) {
        if (!userExists(uid))
            addUser(uid);

        final var guildObj = getGuildObject();
        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        final var reminderObj = new JSONObject()
                .put(Fields.REMINDER.toString(), reminder)
                .put(Fields.REMINDER_CHANNEL.toString(), channelID)
                .put(Fields.REMINDER_TIME.toString(), reminderTime);

        if (timeZone != null)
            reminderObj.put(Fields.REMINDER_TIMEZONE.toString(), timeZone);

        userReminders.put(reminderObj);
        getCache().updateGuild(guildObj);
    }

    public void removeReminder(long uid, int id) {
        if (!userHasReminders(uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var guildObj = getGuildObject();

        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        if (id < 0 || id > userReminders.length() - 1)
            throw new IllegalArgumentException("The ID provided is invalid!");

        userReminders.remove(id);

        getCache().updateGuild(guildObj);
    }

    public void clearReminders(long uid) {
        if (!userHasReminders(uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var guildObj = getGuildObject();

        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        final var reminderScheduler = new ReminderScheduler(guild);
        for (int i = 0; i < userReminders.length(); i++)
            reminderScheduler.removeReminder(uid, i);

        userReminders.clear();

        getCache().updateGuild(guildObj);
    }

    public void removeAllReminderChannels(long uid) {
        if (!userExists(uid))
            throw new NullPointerException("This user doesn't have any reminders!");

        final var guildObj = getGuildObject();
        final var allReminders = getAllReminders(guildObj, uid);

        for (var reminder : allReminders)
            ((JSONObject) reminder).put(Fields.REMINDER_CHANNEL.toString(), -1L);

        getCache().updateGuild(guildObj);
    }

    public void removeReminderChannel(long uid, int id) {
        editReminderChannel(uid, id, -1L);
    }

    public void editReminderChannel(long uid, int id, long channelID) {
        if (!userExists(uid))
            throw new NullPointerException("This user doesn't have any reminders to edit!");

        final var guildObj = getGuildObject();

        JSONObject reminder = getSpecificReminder(guildObj, uid, id);
        reminder.put(Fields.REMINDER_CHANNEL.toString(), channelID);

        getCache().updateGuild(guildObj);
    }

    public void editReminderTime(long uid, int id, long time) {
        if (!userExists(uid))
            throw new NullPointerException("This user doesn't have any reminders to edit!");

        final var guildObj = getGuildObject();

        JSONObject reminder = getSpecificReminder(guildObj, uid, id);
        reminder.put(Fields.REMINDER_TIME.toString(), time);

        getCache().updateGuild(guildObj);
    }

    public void banUser(long uid) {
        setBanState(uid, true);
    }

    public void unbanUser(long uid) {
        setBanState(uid, false);
    }

    private void setBanState(long uid, boolean state) {
        if (state) {
            if (userIsBanned(uid))
                throw new IllegalStateException("This user is already banned!");
        } else {
            if (!userIsBanned(uid))
                throw new IllegalStateException("This user is not banned!");
        }

        final var guildObj = getGuildObject();
        final var usersArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userObj = usersArr.getJSONObject(getIndexOfObjectInArray(usersArr, Fields.USER_ID, uid));

        userObj.put(Fields.IS_BANNED.toString(), state);

        getCache().updateGuild(guildObj);
    }

    public boolean userIsBanned(long uid) {
        if (!userExists(uid))
            addUser(uid);
        return getUser(uid).isBanned();
    }

    public boolean userHasReminders(long uid) {
        if (!userExists(uid))
            return false;
        return getReminders(uid) != null;
    }

    public List<Reminder> getReminders(long uid) {
        return Collections.unmodifiableList(getUser(uid).getReminders());
    }

    public ReminderUser getUser(long uid) {
        JSONObject guildObject = getGuildObject();

        JSONObject reminderObj;

        try {
            reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
        } catch (JSONException e) {
            update();
            try {
                reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
            } catch (JSONException e2) {
                reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
            }
        }

        final var users = reminderObj.getJSONArray(Fields.USERS.toString());

        try {
            final var userObj = users.getJSONObject(getIndexOfObjectInArray(users, Fields.USER_ID, uid));
            final var reminders = userObj.getJSONArray(Fields.USER_REMINDERS.toString());
            final var isBanned = userObj.getBoolean(Fields.IS_BANNED.toString());

            final List<Reminder> ret = new ArrayList<>();

            int i = 0;
            for (var reminder : reminders) {
                final var actualObj = (JSONObject) reminder;
                ret.add(new Reminder(
                        i++,
                        actualObj.getString(Fields.REMINDER.toString()),
                        uid,
                        actualObj.getLong(Fields.REMINDER_CHANNEL.toString()),
                        actualObj.getLong(Fields.REMINDER_TIME.toString()),
                        actualObj.has(Fields.REMINDER_TIMEZONE.toString()) ?
                                actualObj.getString(Fields.REMINDER_TIMEZONE.toString()) :
                                null
                ));
            }
            return new ReminderUser(uid, gid, ret, isBanned);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public List<ReminderUser> getAllGuildUsers() {
        JSONObject guildObject = getGuildObject();

        JSONObject reminderObj;

        try {
            reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
        } catch (JSONException e) {
            return new ArrayList<>();
        }

        final var users = reminderObj.getJSONArray(Fields.USERS.toString());

        try {
            final List<ReminderUser> reminderUsers = new ArrayList<>();

            for (var userObj : users) {
                final var actualUser = (JSONObject) userObj;

                final var uid = actualUser.getLong(Fields.USER_ID.toString());
                final var reminders = actualUser.getJSONArray(Fields.USER_REMINDERS.toString());
                final var isBanned = actualUser.getBoolean(Fields.IS_BANNED.toString());

                final List<Reminder> reminderList = new ArrayList<>();

                int i = 0;
                for (var reminder : reminders) {
                    final var actualObj = (JSONObject) reminder;
                    reminderList.add(new Reminder(
                            i++,
                            actualObj.getString(Fields.REMINDER.toString()),
                            uid,
                            actualObj.getLong(Fields.REMINDER_CHANNEL.toString()),
                            actualObj.getLong(Fields.REMINDER_TIME.toString()),
                            actualObj.has(Fields.REMINDER_TIMEZONE.toString()) ?
                                    actualObj.getString(Fields.REMINDER_TIMEZONE.toString()) :
                                    null
                    ));
                }
                reminderUsers.add(new ReminderUser(uid, gid, reminderList, isBanned));
            }
            return reminderUsers;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean guildHasReminders() {
        return !getAllGuildUsers().isEmpty();
    }

    public void banChannel(long cid) {
        if (channelIsBanned(cid))
            throw new IllegalStateException("This channel is already banned!");

        final var guildObj = getGuildObject();
        final var channelsArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.BANNED_CHANNELS.toString());

        channelsArr.put(cid);

        getCache().updateGuild(guildObj);
    }

    public void unbanChannel(long cid) {
        if (!channelIsBanned(cid))
            throw new IllegalStateException("This channel is not banned!");

        final var guildObj = getGuildObject();
        final var channelsArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.BANNED_CHANNELS.toString());

        channelsArr.remove(getIndexOfObjectInArray(channelsArr, cid));

        getCache().updateGuild(guildObj);
    }

    public boolean channelIsBanned(long cid) {
        final var guildObj = getGuildObject();

        try {
            JSONArray array = guildObj.getJSONObject(Fields.REMINDERS.toString())
                    .getJSONArray(Fields.BANNED_CHANNELS.toString());
            List<Object> list = array.toList();

            return list.stream().anyMatch(obj -> ((long) obj) == cid);
        } catch (JSONException e) {
            update();
            return false;
        }
    }

    private boolean userExists(long uid) {
        return getUser(uid) != null;
    }

    private JSONObject getSpecificReminder(long uid, int id) {
        return getSpecificReminder(getGuildObject(), uid, id);
    }

    private JSONObject getSpecificReminder(JSONObject guildObj, long uid, int id) {
        if (!userHasReminders(uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        return userReminders.getJSONObject(id);
    }

    private JSONArray getAllReminders(JSONObject guildObj, long uid) {
        if (!userHasReminders(uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        return userReminders;
    }

    @Override
    public void update() {
        JSONObject guildObject = getGuildObject();

        if (!guildObject.has(Fields.REMINDERS.toString())) {
            JSONObject reminderObj = new JSONObject();

            reminderObj.put(Fields.USERS.toString(), new JSONArray());
            reminderObj.put(Fields.BANNED_CHANNELS.toString(), new JSONArray());

            guildObject.put(Fields.REMINDERS.toString(), reminderObj);
        }

        getCache().updateGuild(guildObject);
    }

    public enum Fields implements GenericJSONField {
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

        @Override
        public String toString() {
            return super.name().toLowerCase();
        }
    }
}
