package main.commands.slashcommands.commands.misc.reminders;

import main.constants.Toggles;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.reminders.ReminderUser;
import main.utils.json.reminders.RemindersConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ReminderScheduler {
    private final static Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    private static ReminderScheduler INSTANCE;
    private final HashMap<String, ScheduledFuture<?>> scheduledReminders;
    private final ScheduledExecutorService scheduler;

    private ReminderScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduledReminders = new HashMap<>();
    }

    public void scheduleAllReminders() {
        for (var guild : Robertify.shardManager.getGuilds())
            scheduleGuildReminders(guild);
    }

    public void scheduleGuildReminders(Guild guild) {
        RemindersConfig config = new RemindersConfig(guild);

        if (!config.guildHasReminders())
            return;

        List<ReminderUser> allGuildUsers = config.getAllGuildUsers();

        for (var user : allGuildUsers) {
            final var reminders = user.getReminders();

            for (var reminder : reminders)
                scheduleReminder(
                        user.getId(),
                        user.getGuildID(),
                        reminder.getChannelID(),
                        reminder.getReminderTime(),
                        reminder.getReminder(),
                        reminder.getId()
                );
        }
    }

    public void scheduleReminder(long user, long guildID, long destination, long timeOfDay, String reminder, int reminderID) {
        final Guild guild = Robertify.shardManager.getGuildById(guildID);
        final LocalDateTime now = LocalDateTime.now();
        final int currentHour = now.getHour();
        final int currentMinute = now.getMinute();
        final int currentSecond = now.getSecond();

        final long currentTimeInMillis = TimeUnit.HOURS.toMillis(currentHour)
                + TimeUnit.MINUTES.toMillis(currentMinute)
                + TimeUnit.SECONDS.toMillis(currentSecond);

        if (guild == null)
            throw new NullPointerException("Why is the guild invalid??");

        long initialDelay = (timeOfDay - currentTimeInMillis) < 0 ? (86400000 - currentTimeInMillis) + timeOfDay : timeOfDay - currentTimeInMillis;
        ScheduledFuture<?> scheduledReminder = scheduler.scheduleWithFixedDelay(() -> {
                    if (currentTimeInMillis > timeOfDay)
                        return;

                    if (destination == -1L) {
                        dmReminder(guild, user, reminder);
                        return;
                    }

                    if (new RemindersConfig(guild).userIsBanned(user)) {
                        dmReminder(guild, user, reminder);
                        return;
                    }

                    TextChannel channel = guild.getTextChannelById(destination);

                    if (channel == null) {
                        dmReminder(guild, user, reminder);
                        return;
                    }

                    if (!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_SEND)) {
                        dmReminder(guild, user, reminder);
                        return;
                    }

                    if (!new TogglesConfig(guild).getToggle(Toggles.REMINDERS))
                        return;

                    final var localeManager = LocaleManager.getLocaleManager(guild);
                    channel.sendMessage(localeManager.getMessage(RobertifyLocaleMessage.ReminderMessages.REMINDER_SEND, Pair.of("{user}", GeneralUtils.toMention(guild, user, GeneralUtils.Mentioner.USER))))
                            .setEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, localeManager.getMessage(RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE), reminder)
                                    .setTimestamp(Instant.now())
                                    .build())
                            .queue();
                }, initialDelay, 86400000, TimeUnit.MILLISECONDS
        );

        logger.debug("Scheduled a reminder for user with ID: {}, to be sent at {}({} ms from now)", user, timeOfDay, initialDelay);

        scheduledReminders.put(user + ":" + reminderID, scheduledReminder);
    }

    public void removeReminder(long user, int reminderID) {
        ScheduledFuture<?> scheduledReminder = scheduledReminders.get(user + ":" + reminderID);

        if (scheduledReminder != null) {
            scheduledReminder.cancel(false);
            scheduledReminders.remove(user + ":" + reminderID);
        }
    }

    public void editReminder(long guildID, long channelID, long user, int reminderID, long newTime, String reminder) {
        removeReminder(user, reminderID);
        scheduleReminder(user, guildID, channelID, newTime, reminder, reminderID);
    }

    private void dmReminder(Guild guild, long user, String reminder) {
        final var u = Robertify.shardManager.retrieveUserById(user).complete();
        final var localeManager = LocaleManager.getLocaleManager(guild);

        u.openPrivateChannel().queue(channel ->
                channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, localeManager.getMessage(RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE), reminder)
                        .setFooter(localeManager.getMessage(RobertifyLocaleMessage.ReminderMessages.REMINDER_FROM, Pair.of("{server}", guild.getName())))
                        .setTimestamp(Instant.now())
                        .build())
                .queue(null, new ErrorHandler()
                        .handle(ErrorResponse.CANNOT_SEND_TO_USER, ignored -> {}))
        );
    }

    public static ReminderScheduler getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ReminderScheduler();
        return INSTANCE;
    }
}
