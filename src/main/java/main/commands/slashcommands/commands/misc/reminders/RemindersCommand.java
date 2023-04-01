package main.commands.slashcommands.commands.misc.reminders;

import lombok.SneakyThrows;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.constants.Toggles;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.reminders.Reminder;
import main.utils.json.reminders.ReminderUser;
import main.utils.json.reminders.RemindersConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class RemindersCommand extends AbstractSlashCommand implements ICommand {
    private final static Logger logger = LoggerFactory.getLogger(RemindersCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var args = ctx.getArgs();
        final String prefix = new GuildConfig(guild).getPrefix();

        if (!TogglesConfig.getConfig(guild).getToggle(Toggles.REMINDERS))
            return;

        if (args.isEmpty()) {
            list(msg);
        } else {
            switch (args.get(0).toLowerCase()) {
                case "add" -> add(msg, args);
                case "remove" -> remove(msg, args);
                case "clear" -> clear(msg);
                case "list" -> list(msg);
                case "edit" -> edit(msg, args);
                case "banuser" -> banUser(msg, args);
                case "unbanuser" -> unbanUser(msg, args);
                case "banchannel" -> banChannel(msg, args);
                case "unbanchannel" -> unbanChannel(msg, args);
                default ->
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_ARGS
                                        + getUsages(prefix)).build())
                                .queue();
            }
        }
    }

    private void add(Message msg, List<String> args) {
        final Guild guild = msg.getGuild();
        final String reminder;
        final String timeString;
        final Long channel;

        if (args.size() < 3) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                            You do not have enough arguments!
                                                        
                            **__Usage__**
                            `reminders add <(00:00AM/PM)|00:00> [#channel] <reminder>`
                                                        
                            **For Example**
                            `reminders add 12:45PM #robertify-reminders Robertify is so cool!`
                            *This will remind you everyday at 12:45 EST in the channel "roberitfy-reminders" that Robertify is so cool!*
                            """).build())
                    .queue();
            return;
        }

        timeString = args.get(1);

        if (GeneralUtils.stringIsID(args.get(2))) {
            channel = Long.parseLong(GeneralUtils.getDigitsOnly(args.get(2)));
            reminder = GeneralUtils.getJoinedString(args, 3);
        } else {
            channel = null;
            reminder = GeneralUtils.getJoinedString(args, 2);
        }

        msg.replyEmbeds(handleAdd(guild, msg.getAuthor(), reminder, timeString, channel))
                .queue();
    }

    @SneakyThrows
    private MessageEmbed handleAdd(Guild guild, User user, String reminder, String time, @Nullable Long channelID) {
        RemindersConfig remindersConfig = new RemindersConfig(guild);

        channelID = channelID == null ? -1L : channelID;

        if (remindersConfig.channelIsBanned(channelID))
            return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                            RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                            RobertifyLocaleMessage.ReminderMessages.CANNOT_SET_BANNED_REMINDER_CHANNEL,
                            Pair.of("{channel}", GeneralUtils.toMention(guild, channelID, GeneralUtils.Mentioner.CHANNEL))
                    )
                    .build();

        final var channel = guild.getTextChannelById(channelID);
        final var selfMember = guild.getSelfMember();

        final long finalChannelID = channelID;
        final var result = guild.retrieveMemberById(user.getIdLong())
                .submit()
                .thenApply(member -> {
                    if (channel != null) {
                        if (!selfMember.hasPermission(channel, net.dv8tion.jda.api.Permission.MESSAGE_SEND)) {
                            return RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, "I do not have enough permissions to send your reminder in: " + channel.getAsMention())
                                    .build();
                        }

                        if (!member.hasPermission(channel, net.dv8tion.jda.api.Permission.MESSAGE_SEND)) {
                            return RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, "You do not have enough permissions to send your reminder in: " + channel.getAsMention())
                                    .build();
                        }

                    }

                    long timeInMillis = 0;
                    int hour = 0, minute = 0;

                    try {
                        timeInMillis = timeToMillis(time);
                        hour = extractTime(time, TimeUnit.HOURS);
                        minute = extractTime(time, TimeUnit.MINUTES);
                    } catch (IllegalArgumentException e) {
                        if (e.getMessage().contains("minute")) {
                            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_MINUTE).build();
                        } else if (e.getMessage().contains("hour")) {
                            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_HOUR).build();
                        } else if (e.getMessage().contains("time")) {
                            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_INVALID_TIME_FORMAT).build();
                        }
                    }

                    remindersConfig.addReminder(
                            user.getIdLong(),
                            reminder,
                            finalChannelID,
                            timeInMillis
                    );

                    new ReminderScheduler(guild)
                            .scheduleReminder(
                                    user.getIdLong(),
                                    finalChannelID,
                                    hour,
                                    minute,
                                    reminder,
                                    remindersConfig.getReminders(user.getIdLong()).size() - 1
                            );

                    return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_ADDED).build();
                });
        return result.join();
    }

    private void remove(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide an ID of a reminder to remove!").build())
                    .queue();
            return;
        }

        final var idStr = args.get(1);

        if (!GeneralUtils.stringIsNum(idStr)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid integer as an ID!")
                            .build())
                    .queue();
            return;
        }

        final var id = Integer.parseInt(idStr);

        msg.replyEmbeds(handleRemove(guild, msg.getAuthor(), id - 1)).queue();
    }

    private MessageEmbed handleRemove(Guild guild, User user, int id) {
        final var config = new RemindersConfig(guild);

        List<Reminder> reminders = config.getReminders(user.getIdLong());

        if (reminders.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS)
                    .build();

        if (id < 0 || id > reminders.size() - 1)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.INVALID_REMINDER_ID)
                    .build();

        config.removeReminder(user.getIdLong(), id);

        new ReminderScheduler(guild)
                .removeReminder(user.getIdLong(), id);

        return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                        RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                        RobertifyLocaleMessage.ReminderMessages.REMINDER_REMOVED,
                        Pair.of("{reminder}", reminders.get(id).getReminder())
                )
                .build();
    }

    private void clear(Message msg) {
        msg.replyEmbeds(handleClear(msg.getGuild(), msg.getAuthor()))
                .queue();
    }

    private MessageEmbed handleClear(Guild guild, User user) {
        final var config = new RemindersConfig(guild);

        try {
            config.clearReminders(user.getIdLong());
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_CLEARED)
                    .build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS).build();
        } catch (Exception e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR).build();
        }
    }

    private void list(Message msg) {
        msg.replyEmbeds(handleList(msg.getGuild(), msg.getAuthor()))
                .queue();
    }

    private MessageEmbed handleList(Guild guild, User user) {
        RemindersConfig config = new RemindersConfig(guild);

        if (!config.userHasReminders(user.getIdLong()))
            return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                    RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                    RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS
            ).build();

        final var reminders = config.getReminders(user.getIdLong());

        if (reminders.isEmpty())
            return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                    RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                    RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS
            ).build();

        final var sb = new StringBuilder();


        for (int i = 0; i < reminders.size(); i++) {
            Reminder reminder = reminders.get(i);
            sb.append("**").append(i + 1).append(".** - ")
                    .append(reminder.getReminder())
                    .append(" <t:")
                    .append(getNextUNIXTimestamp(reminder))
                    .append(":t>")
                    .append(" (<t:")
                    .append(getNextUNIXTimestamp(reminder))
                    .append(":R>)")
                    .append("\n");
        }

        return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                sb.toString()
        ).build();
    }

    private long getNextUNIXTimestamp(Reminder reminder) {
        long todaysTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() + TimeUnit.MILLISECONDS.toSeconds(reminder.getReminderTime());
        long tomorrowsTime = todaysTime + 86400L;
        return todaysTime < TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) ? tomorrowsTime : todaysTime;
    }

    private void edit(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide more arguments!\n\n" +
                            "**__Valid Arguments__**\n" +
                            "`reminders edit channel <ID> <#channel>`\n" +
                            "`reminders edit time <ID> <(00:00AM/PM)|00:00>`").build())
                    .queue();
            return;
        }

        switch (args.get(1).toLowerCase()) {
            case "channel" -> {
                if (args.size() < 4) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the reminder ID and the channel ID!").build())
                            .queue();
                    return;
                }

                final var reminderIDStr = args.get(2);
                final var channelIDStr = args.get(3);

                if (!GeneralUtils.stringIsInt(reminderIDStr)) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The reminder ID must be a valid integer!").build())
                            .queue();
                    return;
                }

                if (!GeneralUtils.stringIsID(channelIDStr)) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The channel ID must be a valid id!").build())
                            .queue();
                    return;
                }

                TextChannel channel = Robertify.shardManager.getTextChannelById(GeneralUtils.getDigitsOnly(channelIDStr));

                if (channel == null) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid channel!").build())
                            .queue();
                    return;
                }

                final var id = Integer.parseInt(reminderIDStr);

                msg.replyEmbeds(handleChannelEdit(guild, msg.getAuthor(), id - 1, channel.getIdLong()))
                        .queue();
            }
            case "time" -> {
                if (args.size() < 4) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the reminder ID and the new time!").build())
                            .queue();
                    return;
                }

                final var reminderIDStr = args.get(2);
                final var timeStr = args.get(3);

                if (!GeneralUtils.stringIsInt(reminderIDStr)) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The reminder ID must be a valid integer!")
                                    .build())
                            .queue();
                    return;
                }

                msg.replyEmbeds(handleTimeEdit(guild, msg.getAuthor(), (Integer.parseInt(reminderIDStr)) - 1, timeStr))
                        .queue();
            }
        }
    }

    private MessageEmbed handleChannelEdit(Guild guild, User user, int id, Long channelID) {
        final var config = new RemindersConfig(guild);

        if (!config.userHasReminders(user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS).build();

        try {
            List<Reminder> reminders = config.getReminders(user.getIdLong());

            if (id < 0 || id > reminders.size())
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDER_WITH_ID).build();

            final var channel = guild.getTextChannelById(channelID);
            final var selfMember = guild.getSelfMember();
            final AtomicReference<MessageEmbed> retEmbed = new AtomicReference<>();
            guild.retrieveMemberById(user.getIdLong()).queue(member -> {
                if (channel != null) {
                    if (!selfMember.hasPermission(channel, net.dv8tion.jda.api.Permission.MESSAGE_SEND)) {
                        retEmbed.set(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, "I do not have enough permissions to send your reminder in: " + channel.getAsMention())
                                .build());
                        return;
                    }

                    if (!member.hasPermission(channel, net.dv8tion.jda.api.Permission.MESSAGE_SEND)) {
                        retEmbed.set(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, "You do not have enough permissions to send your reminder in: " + channel.getAsMention())
                                .build());
                        return;
                    }

                }

                config.editReminderChannel(user.getIdLong(), id, channelID);

                Reminder reminder = reminders.get(id);

                new ReminderScheduler(guild)
                        .editReminder(
                                channelID,
                                reminder.getUserId(),
                                reminder.getId(),
                                reminder.getHour(),
                                reminder.getMinute(),
                                reminder.getReminder()
                        );

                if (channelID == -1L)
                    retEmbed.set(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_REMOVED, Pair.of("{id}", String.valueOf(id + 1))).build());
                else
                    retEmbed.set(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_CHANNEL_CHANGED,
                            Pair.of("{id}", String.valueOf(id + 1)),
                            Pair.of("{channel}", GeneralUtils.toMention(guild, channelID, GeneralUtils.Mentioner.CHANNEL))
                    ).build());
            });
            return retEmbed.get();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS).build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR).build();
        }
    }

    private MessageEmbed handleTimeEdit(Guild guild, User user, int id, String timeUnparsed) {
        long timeInMillis = 0;
        int hour = 0, minute = 0;

        try {
            timeInMillis = timeToMillis(timeUnparsed);
            hour = extractTime(timeUnparsed, TimeUnit.HOURS);
            minute = extractTime(timeUnparsed, TimeUnit.MINUTES);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("minute")) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_MINUTE).build();
            } else if (e.getMessage().contains("hour")) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_HOUR).build();
            } else if (e.getMessage().contains("time")) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_INVALID_TIME_FORMAT).build();
            }
        }

        final var config = new RemindersConfig(guild);

        if (!config.userHasReminders(user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS)
                    .build();

        List<Reminder> reminders = config.getReminders(user.getIdLong());

        if (id < 0 || id >= reminders.size())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.MISSING_REMINDER_ID)
                    .build();

        try {
            config.editReminderTime(user.getIdLong(), id, timeInMillis);

            Reminder reminder = reminders.get(id);

            new ReminderScheduler(guild)
                    .editReminder(
                            reminder.getChannelID(),
                            reminder.getUserId(),
                            reminder.getId(),
                            hour,
                            minute,
                            reminder.getReminder()
                    );
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_TIME_CHANGED,
                            Pair.of("{time}", timeUnparsed),
                            Pair.of("{id}", String.valueOf(id + 1))
                    )
                    .build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS)
                    .build();
        } catch (Exception e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR).build();
        }
    }

    private long timeToMillis(String time) {
        long timeInMillis = 0;
        final var timeSplit = splitTime(time);

        timeInMillis += TimeUnit.MINUTES.toMillis(timeSplit.getRight());
        timeInMillis += TimeUnit.HOURS.toMillis(timeSplit.getLeft());

        return timeInMillis;
    }

    private int extractTime(String time, TimeUnit unit) {
        final var timeSplit = splitTime(time);

        switch (unit) {
            case HOURS -> {
                return timeSplit.getLeft();
            }
            case MINUTES -> {
                return timeSplit.getRight();
            }
            default -> throw new IllegalArgumentException("Invalid time to extract!");
        }
    }

    private Pair<Integer, Integer> splitTime(String time) {
        int hour, minute;
        String meridiemIndicator;
        if (Pattern.matches("^\\d{1,2}:\\d{1,2}(AM|PM|am|pm)$", time)) {
            String[] split = time.split(":");
            hour = Integer.parseInt(split[0]);

            if (hour < 0 || hour > 12)
                throw new IllegalArgumentException("Invalid hour");

            minute = Integer.parseInt(GeneralUtils.getDigitsOnly(split[1]));

            if (minute < 0 || minute > 59)
                throw new IllegalArgumentException("Invalid minute");

            meridiemIndicator = GeneralUtils.removeAllDigits(split[1]);

            if (hour == 12) {
                if (meridiemIndicator.equalsIgnoreCase("am"))
                    hour = 0;
            } else if (meridiemIndicator.equalsIgnoreCase("pm"))
                hour += 12;
        } else if (Pattern.matches("^\\d{1,2}:\\d{1,2}$", time)) {
            String[] split = time.split(":");
            hour = Integer.parseInt(split[0]);

            if (hour < 0 || hour > 24)
                throw new IllegalArgumentException("Invalid hour");

            minute = Integer.parseInt(GeneralUtils.getDigitsOnly(split[1]));

            if (minute < 0 || minute > 59)
                throw new IllegalArgumentException("Invalid minute");
        } else
            throw new IllegalArgumentException("Invalid time format!");
        return Pair.of(hour, minute);
    }

    private void banUser(Message msg, List<String> args) {
        Guild guild = msg.getGuild();
        if (!GeneralUtils.hasPerms(guild, msg.getMember(), Permission.ROBERTIFY_ADMIN)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                    .queue();
            return;
        }

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a user to ban!").build())
                    .queue();
            return;
        }

        final var userID = args.get(1);

        if (!GeneralUtils.stringIsID(userID)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user ID!").build())
                    .queue();
            return;
        }

        Member member = guild.retrieveMemberById(GeneralUtils.getDigitsOnly(userID)).complete();

        if (member == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to a user in this server!").build())
                    .queue();
            return;
        }

        msg.replyEmbeds(handleUserBan(guild, member.getIdLong())).queue();
    }

    private MessageEmbed handleUserBan(Guild guild, long idToBan) {
        final var config = new RemindersConfig(guild);

        if (config.userIsBanned(idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.BanMessages.USER_ALREADY_BANNED).build();

        config.banUser(idToBan);
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.BanMessages.USER_PERM_BANNED_RESPONSE, Pair.of("{user}", GeneralUtils.toMention(guild, idToBan, GeneralUtils.Mentioner.USER)))
                .build();
    }

    private void unbanUser(Message msg, List<String> args) {
        Guild guild = msg.getGuild();
        if (!GeneralUtils.hasPerms(guild, msg.getMember(), Permission.ROBERTIFY_ADMIN)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                    .queue();
            return;
        }

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a user to unban!").build())
                    .queue();
            return;
        }

        final var userID = args.get(1);

        if (!GeneralUtils.stringIsID(userID)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user ID!").build())
                    .queue();
            return;
        }

        Member member = guild.retrieveMemberById(GeneralUtils.getDigitsOnly(userID)).complete();

        if (member == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to a user in this server!").build())
                    .queue();
            return;
        }

        msg.replyEmbeds(handleUserUnBan(guild, member.getIdLong())).queue();
    }

    private MessageEmbed handleUserUnBan(Guild guild, long idToBan) {
        final var config = new RemindersConfig(guild);

        if (!config.userIsBanned(idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.USER_NOT_BANNED).build();

        config.unbanUser(idToBan);
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.USER_UNBANNED_RESPONSE, Pair.of("{user}", GeneralUtils.toMention(guild, idToBan, GeneralUtils.Mentioner.USER)))
                .build();
    }

    private void banChannel(Message msg, List<String> args) {
        Guild guild = msg.getGuild();
        if (!GeneralUtils.hasPerms(guild, msg.getMember(), Permission.ROBERTIFY_ADMIN)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                    .queue();
            return;
        }

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a channel to ban!").build())
                    .queue();
            return;
        }

        final var channelID = args.get(1);

        if (!GeneralUtils.stringIsID(channelID)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid channel ID!").build())
                    .queue();
            return;
        }

        TextChannel channel = guild.getTextChannelById(GeneralUtils.getDigitsOnly(channelID));

        if (channel == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to a channel in this server!").build())
                    .queue();
            return;
        }

        msg.replyEmbeds(handleChannelBan(guild, channel.getIdLong())).queue();
    }

    private MessageEmbed handleChannelBan(Guild guild, long idToBan) {
        final var config = new RemindersConfig(guild);

        if (config.channelIsBanned(idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_CHANNEL_ALREADY_BANNED).build();

        config.banChannel(idToBan);
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.BanMessages.USER_PERM_BANNED_RESPONSE, Pair.of("{user}", GeneralUtils.toMention(guild, idToBan, GeneralUtils.Mentioner.CHANNEL)))
                .build();
    }

    private void unbanChannel(Message msg, List<String> args) {
        Guild guild = msg.getGuild();
        if (!GeneralUtils.hasPerms(guild, msg.getMember(), Permission.ROBERTIFY_ADMIN)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                    .queue();
            return;
        }

        if (args.size() < 2) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a channel to unban!").build())
                    .queue();
            return;
        }

        final var channelID = args.get(1);

        if (!GeneralUtils.stringIsID(channelID)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid channel ID!").build())
                    .queue();
            return;
        }

        TextChannel channel = guild.getTextChannelById(GeneralUtils.getDigitsOnly(channelID));

        if (channel == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That ID doesn't belong to a channel in this server!").build())
                    .queue();
            return;
        }

        msg.replyEmbeds(handleChannelUnBan(guild, channel.getIdLong())).queue();
    }

    private MessageEmbed handleChannelUnBan(Guild guild, long idToBan) {
        final var config = new RemindersConfig(guild);

        if (!config.channelIsBanned(idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_CHANNEL_NOT_BANNED).build();

        config.unbanChannel(idToBan);
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.USER_UNBANNED_RESPONSE, Pair.of("{user}", GeneralUtils.toMention(guild, idToBan, GeneralUtils.Mentioner.CHANNEL)))
                .build();
    }

    @Override
    public String getName() {
        return "reminders";
    }

    @Override
    public List<String> getAliases() {
        return List.of("reminder", "remind");
    }

    @Override
    public String getUsages(String prefix) {
        return "\n\n**__Usages__**\n" +
                "\n__General Commands__\n" +
                "`" + prefix + "reminders add <(00:00AM/PM)|00:00> [#channel] <reminder>`\n" +
                "`" + prefix + "reminders remove <id>`\n" +
                "`" + prefix + "reminders clear`\n" +
                "`" + prefix + "reminders list`\n" +
                "`" + prefix + "reminders edit channel <ID> <#channel>`\n" +
                "`" + prefix + "reminders edit time  <ID> <(00:00AM/PM)|00:00>`\n" +
                "\n__Admin Commands__\n" +
                "`" + prefix + "reminders banchannel <channel>`\n" +
                "`" + prefix + "reminders unbanchannel <channel>`\n" +
                "`" + prefix + "reminders banuser <user>`\n" +
                "`" + prefix + "reminders unbanuser <user>`\n\n" +
                "**NOTE**\n" +
                "*<> - Required*\n" +
                "*[] - Optional*";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `" + GeneralUtils.listToString(getAliases()) + "`" +
                "\n\n[insert description]"
                + getUsages(prefix);
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("reminders")
                        .setDescription("Set your reminders!")
                        .addSubCommands(
                                SubCommand.of(
                                        "add",
                                        "Add a reminder!",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "time",
                                                        "The time to remind you at daily!",
                                                        true
                                                ),
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "reminder",
                                                        "What you want to be reminded of",
                                                        true
                                                ),
                                                CommandOption.of(
                                                        OptionType.CHANNEL,
                                                        "channel",
                                                        "The channel to send the reminder in",
                                                        false
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "list",
                                        "List all your reminders"
                                ),
                                SubCommand.of(
                                        "remove",
                                        "Remove a specific reminder",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.INTEGER,
                                                        "id",
                                                        "The ID of the reminder to remove",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "clear",
                                        "Clear all reminders"
                                )
                        )
                        .addSubCommandGroups(
                                SubCommandGroup.of(
                                        "edit",
                                        "Edit your reminders!",
                                        List.of(
                                                SubCommand.of(
                                                        "channel",
                                                        "Edit the channel a specific reminder gets sent in!",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.INTEGER,
                                                                        "id",
                                                                        "The ID of the reminder to edit",
                                                                        true
                                                                ),
                                                                CommandOption.of(
                                                                        OptionType.CHANNEL,
                                                                        "channel",
                                                                        "The new channel to send the reminder in",
                                                                        false
                                                                )
                                                        )
                                                ),
                                                SubCommand.of(
                                                        "time",
                                                        "Edit the time a specific reminder gets sent at!",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.INTEGER,
                                                                        "id",
                                                                        "The ID of the reminder to edit",
                                                                        true
                                                                ),
                                                                CommandOption.of(
                                                                        OptionType.STRING,
                                                                        "time",
                                                                        "The new time to send the reminder at",
                                                                        true
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                SubCommandGroup.of(
                                        "ban",
                                        "Ban either users or channels",
                                        List.of(
                                                SubCommand.of(
                                                        "channel",
                                                        "Ban a specific channel from being used!",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.CHANNEL,
                                                                        "channel",
                                                                        "The channel to ban",
                                                                        true
                                                                )
                                                        )
                                                ),
                                                SubCommand.of(
                                                        "user",
                                                        "Ban a specific user from receiving reminders in this server!",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.USER,
                                                                        "user",
                                                                        "The user to ban",
                                                                        true
                                                                )
                                                        )
                                                )
                                        )
                                )
                                ,
                                SubCommandGroup.of(
                                        "unban",
                                        "Unban either users or channels",
                                        List.of(
                                                SubCommand.of(
                                                        "channel",
                                                        "Unban a specific channel from being used!",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.CHANNEL,
                                                                        "channel",
                                                                        "The channel to unban",
                                                                        true
                                                                )
                                                        )
                                                ),
                                                SubCommand.of(
                                                        "user",
                                                        "Unban a specific user!",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.USER,
                                                                        "user",
                                                                        "The user to unban",
                                                                        true
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "[insert description]"
                + getUsages();
    }

    @Override
    public String getUsages() {
        return """


                **__Usages__**

                __General Commands__
                `/reminders add <(00:00AM/PM)|00:00> [#channel] <reminder>`
                `/reminders remove <id>`
                `/reminders clear`
                `/reminders list`
                `/reminders edit channel <ID> <#channel>`
                `/reminders edit time  <ID> <(00:00AM/PM)|00:00>`

                __Admin Commands__
                `/reminders banchannel <channel>`
                `/reminders unbanchannel <channel>`
                `/reminders banuser <user>`
                `/reminders unbanuser <user>`

                **NOTE**
                *<> - Required*
                *[] - Optional*""";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final Guild guild = event.getGuild();
        if (!TogglesConfig.getConfig(guild).getToggle(Toggles.REMINDERS)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DISABLED_FEATURE).build())
                    .queue();
            return;
        }

        String[] split = event.getFullCommandName().split("\\s");
        List<OptionMapping> options = event.getOptions();

        final Member member = event.getMember();
        final User eventUser = event.getUser();

        switch (split[1]) {
            case "add" -> {
                String time = options.get(0).getAsString();
                String reminder = options.get(1).getAsString();

                MessageChannel channel = null;
                if (options.size() == 3)
                    channel = options.get(2).getAsChannel().asGuildMessageChannel();

                event.replyEmbeds(handleAdd(guild, eventUser, reminder, time, channel != null ? channel.getIdLong() : -1L))
                        .setEphemeral(true)
                        .queue();
            }
            case "remove" -> {
                int id = (int) options.get(0).getAsLong();

                event.replyEmbeds(handleRemove(guild, eventUser, id - 1))
                        .setEphemeral(true)
                        .queue();
            }
            case "edit" -> {
                switch (split[2]) {
                    case "channel" -> {
                        int id = (int) options.get(0).getAsLong();

                        MessageChannel channel = null;
                        if (options.size() == 2)
                            channel = options.get(1).getAsChannel().asGuildMessageChannel();

                        event.replyEmbeds(handleChannelEdit(guild, eventUser, id - 1, channel != null ? channel.getIdLong() : -1L))
                                .setEphemeral(true)
                                .queue();
                    }
                    case "time" -> {
                        int id = (int) options.get(0).getAsLong();
                        String time = options.get(1).getAsString();

                        event.replyEmbeds(handleTimeEdit(guild, eventUser, id - 1, time))
                                .setEphemeral(true)
                                .queue();
                    }
                }
            }
            case "clear" -> event.replyEmbeds(handleClear(guild, eventUser))
                    .setEphemeral(true)
                    .queue();
            case "list" -> event.replyEmbeds(handleList(guild, eventUser))
                    .setEphemeral(true)
                    .queue();
            case "ban" -> {
                if (!GeneralUtils.hasPerms(guild, member, Permission.ROBERTIFY_ADMIN)) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                switch (split[2]) {
                    case "channel" -> {
                        final var channel = options.get(0).getAsChannel().asGuildMessageChannel();

                        event.replyEmbeds(handleChannelBan(guild, channel.getIdLong()))
                                .setEphemeral(true)
                                .queue();
                    }
                    case "user" -> {
                        final var user = options.get(0).getAsUser();

                        event.replyEmbeds(handleUserBan(guild, user.getIdLong()))
                                .setEphemeral(true)
                                .queue();
                    }
                }
            }
            case "unban" -> {
                if (!GeneralUtils.hasPerms(guild, member, Permission.ROBERTIFY_ADMIN)) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                switch (split[2]) {
                    case "channel" -> {
                        final var channel = options.get(0).getAsChannel().asGuildMessageChannel();

                        event.replyEmbeds(handleChannelUnBan(guild, channel.getIdLong()))
                                .setEphemeral(true)
                                .queue();
                    }
                    case "user" -> {
                        final var user = options.get(0).getAsUser();

                        event.replyEmbeds(handleUserUnBan(guild, user.getIdLong()))
                                .setEphemeral(true)
                                .queue();
                    }
                }
            }
        }
    }

    public static void scheduleAllReminders() {
        for (var guild : Robertify.shardManager.getGuilds())
            scheduleGuildReminders(guild);
    }

    public static void unscheduleAllReminders() {
        for (var guild : Robertify.shardManager.getGuilds())
            unscheduleGuildReminders(guild);
    }

    public static void scheduleGuildReminders(Guild guild) {
        CompletableFuture.runAsync(() -> {
            logger.debug("Attempting to schedule guild reminders for {}", guild.getName());

            final var config = new RemindersConfig(guild);
            final var scheduler = new ReminderScheduler(guild);

            if (!config.guildHasReminders()) {
                logger.debug("{} didn't have any reminders to schedule.", guild.getName());
                return;
            }

            List<ReminderUser> allGuildUsers = config.getAllGuildUsers();

            for (var user : allGuildUsers) {
                final var reminders = user.getReminders();

                logger.debug("Attempting to schedule reminder(s) for {} in {}", user.getId(), guild.getName());
                for (var reminder : reminders) {
                    logger.debug(
                            "Scheduling reminder with information:\nUser ID: {}\nChannel ID: {}\n Hour: {}\n Minute: {}\n Reminder: {}\nReminder ID: {}\n\n",
                            user.getId(), reminder.getChannelID(), reminder.getHour(), reminder.getMinute(), reminder.getReminder(), reminder.getId()
                    );
                    scheduler.scheduleReminder(
                            user.getId(),
                            reminder.getChannelID(),
                            reminder.getHour(),
                            reminder.getMinute(),
                            reminder.getReminder(),
                            reminder.getId()
                    );
                }
                logger.debug("Scheduled all {} reminder(s) for {} in {}.", reminders.size(), user.getId(), guild.getName());
            }
        });
    }

    public static void unscheduleGuildReminders(Guild guild) {
        CompletableFuture.runAsync(() -> {
            final var config = new RemindersConfig(guild);
            final var scheduler = new ReminderScheduler(guild);

            if (!config.guildHasReminders())
                return;

            List<ReminderUser> allGuildUsers = config.getAllGuildUsers();

            for (var user : allGuildUsers) {
                final var reminders = user.getReminders();

                for (var reminder : reminders)
                    scheduler.removeReminder(
                            user.getId(),
                            reminder.getId()
                    );
            }
        });
    }
}
