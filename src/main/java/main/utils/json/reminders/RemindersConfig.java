package main.utils.json.reminders;

import main.utils.json.AbstractGuildConfig;
import main.utils.json.GenericJSONField;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemindersConfig extends AbstractGuildConfig {

    public void addUser(long gid, long uid) {
        if (userExists(gid, uid))
            return;

        JSONObject guildObject = getGuildObject(gid);
        final var userArr = guildObject.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());

        final var userObj = new JSONObject();
        userObj.put(Fields.USER_ID.toString(), uid);
        userObj.put(Fields.USER_REMINDERS.toString(), new JSONArray());
        userObj.put(Fields.IS_BANNED.toString(), false);

        userArr.put(userObj);

        getCache().updateGuild(guildObject);
    }

    public void addReminder(long gid, long uid, String reminder, long channelID, long reminderTime) {
        if (!userExists(gid, uid))
            addUser(gid, uid);

        final var guildObj = getGuildObject(gid);
        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        userReminders.put(new JSONObject()
                .put(Fields.REMINDER.toString(), reminder)
                .put(Fields.REMINDER_CHANNEL.toString(), channelID)
                .put(Fields.REMINDER_TIME.toString(), reminderTime)
        );

        getCache().updateGuild(guildObj);
    }

    public void removeReminder(long gid, long uid, int id) {
        if (!userHasReminders(gid, uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var guildObj = getGuildObject(gid);

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

    public void clearReminders(long gid, long uid) {
        if (!userHasReminders(gid, uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var guildObj = getGuildObject(gid);

        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        for (int i = 0; i < userReminders.length(); i++)
            userReminders.remove(i);

        getCache().updateGuild(guildObj);
    }

    public void removeAllReminderChannels(long gid, long uid) {
        if (!userExists(gid, uid))
            throw new NullPointerException("This user doesn't have any reminders!");

        final var guildObj = getGuildObject(gid);
        final var allReminders = getAllReminders(guildObj, gid, uid);

        for (var reminder : allReminders)
            ((JSONObject) reminder).put(Fields.REMINDER_CHANNEL.toString(), -1L);

        getCache().updateGuild(guildObj);
    }

    public void removeReminderChannel(long gid, long uid, int id) {
        editReminderChannel(gid, uid, id, -1L);
    }

    public void editReminderChannel(long gid, long uid, int id, long channelID) {
        if (!userExists(gid, uid))
            throw new NullPointerException("This user doesn't have any reminders to edit!");

        final var guildObj = getGuildObject(gid);

        JSONObject reminder = getSpecificReminder(guildObj, gid, uid, id);
        reminder.put(Fields.REMINDER_CHANNEL.toString(), channelID);

        getCache().updateGuild(guildObj);
    }

    public void editReminderTime(long gid, long uid, int id, long time) {
        if (!userExists(gid, uid))
            throw new NullPointerException("This user doesn't have any reminders to edit!");

        final var guildObj = getGuildObject(gid);

        JSONObject reminder = getSpecificReminder(guildObj, gid, uid, id);
        reminder.put(Fields.REMINDER_TIME.toString(), time);

        getCache().updateGuild(guildObj);
    }

    public void banUser(long gid, long uid) {
        setBanState(gid, uid, true);
    }

    public void unbanUser(long gid, long uid) {
        setBanState(gid, uid, false);
    }

    private void setBanState(long gid, long uid, boolean state) {
        if (state) {
            if (userIsBanned(gid, uid))
                throw new IllegalStateException("This user is already banned!");
        } else {
            if (!userIsBanned(gid, uid))
                throw new IllegalStateException("This user is not banned!");
        }

        final var guildObj = getGuildObject(gid);
        final var usersArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userObj = usersArr.getJSONObject(getIndexOfObjectInArray(usersArr, Fields.USER_ID, uid));

        userObj.put(Fields.IS_BANNED.toString(), state);

        getCache().updateGuild(guildObj);
    }

    public boolean userIsBanned(long gid, long uid) {
        if (!userExists(gid, uid))
            addUser(gid, uid);
        return getUser(gid, uid).isBanned();
    }

    public boolean userHasReminders(long gid, long uid) {
        if (!userExists(gid, uid))
            return false;
        return getReminders(gid, uid) != null;
    }

    public List<Reminder> getReminders(long gid, long uid) {
        return Collections.unmodifiableList(getUser(gid, uid).getReminders());
    }

    public ReminderUser getUser(long gid, long uid) {
        JSONObject guildObject = getGuildObject(gid);

        JSONObject reminderObj;

        try {
            reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
        } catch (JSONException e) {
            update(gid);
            reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
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
                        actualObj.getLong(Fields.REMINDER_TIME.toString())
                ));
            }
            return new ReminderUser(uid, gid, ret, isBanned);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public List<ReminderUser> getAllGuildUsers(long gid) {
        JSONObject guildObject = getGuildObject(gid);

        JSONObject reminderObj;

        try {
            reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
        } catch (JSONException e) {
            update(gid);
            reminderObj = guildObject.getJSONObject(Fields.REMINDERS.toString());
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
                            actualObj.getLong(Fields.REMINDER_TIME.toString())
                    ));
                }
                reminderUsers.add(new ReminderUser(uid, gid, reminderList, isBanned));
            }
            return reminderUsers;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void banChannel(long gid, long cid) {
        if (channelIsBanned(gid, cid))
            throw new IllegalStateException("This channel is already banned!");

        final var guildObj = getGuildObject(gid);
        final var channelsArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.BANNED_CHANNELS.toString());

        channelsArr.put(cid);

        getCache().updateGuild(guildObj);
    }

    public void unbanChannel(long gid, long cid) {
        if (!channelIsBanned(gid, cid))
            throw new IllegalStateException("This channel is not banned!");

        final var guildObj = getGuildObject(gid);
        final var channelsArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.BANNED_CHANNELS.toString());

        channelsArr.remove(getIndexOfObjectInArray(channelsArr, cid));

        getCache().updateGuild(guildObj);
    }

    public boolean channelIsBanned(long gid, long cid) {
        final var guildObj = getGuildObject(gid);

        try {
            JSONArray array = guildObj.getJSONObject(Fields.REMINDERS.toString())
                    .getJSONArray(Fields.BANNED_CHANNELS.toString());
            List<Object> list = array.toList();

            return list.stream().anyMatch(obj -> ((long) obj) == cid);
        } catch (JSONException e) {
            update(gid);
            return false;
        }
    }

    private boolean userExists(long gid, long uid) {
        return getUser(gid, uid) != null;
    }

    private JSONObject getSpecificReminder(long gid, long uid, int id) {
        return getSpecificReminder(getGuildObject(gid), gid, uid, id);
    }

    private JSONObject getSpecificReminder(JSONObject guildObj, long gid, long uid, int id) {
        if (!userHasReminders(gid, uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        return userReminders.getJSONObject(id);
    }

    private JSONArray getAllReminders(JSONObject guildObj, long gid, long uid) {
        if (!userHasReminders(gid, uid))
            throw new NullPointerException("This user doesn't have any reminders in this guild!");

        final var userArr = guildObj.getJSONObject(Fields.REMINDERS.toString())
                .getJSONArray(Fields.USERS.toString());
        final var userReminders = userArr
                .getJSONObject(getIndexOfObjectInArray(userArr, Fields.USER_ID, uid))
                .getJSONArray(Fields.USER_REMINDERS.toString());

        return userReminders;
    }

    @Override
    public void update(long gid) {
        JSONObject guildObject = getGuildObject(gid);

        if (!guildObject.has(Fields.REMINDERS.toString())) {
            JSONObject reminderObj = new JSONObject();

            reminderObj.put(Fields.USERS.toString(), new JSONArray());
            reminderObj.put(Fields.BANNED_CHANNELS.toString(), new JSONArray());

            guildObject.put(Fields.REMINDERS.toString(), reminderObj);
        }

        getCache().updateCache(Document.parse(guildObject.toString()));
    }

    public enum Fields implements GenericJSONField {
        REMINDERS,
        USERS,
        USER_ID,
        USER_REMINDERS,
        REMINDER,
        REMINDER_CHANNEL,
        REMINDER_TIME,
        IS_BANNED,
        BANNED_CHANNELS;

        @Override
        public String toString() {
            return super.name().toLowerCase();
        }
    }
}
