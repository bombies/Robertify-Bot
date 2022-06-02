package main.commands.slashcommands.commands.misc.reminders;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.reminders.Reminder;
import main.utils.json.reminders.RemindersConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RemindersCommand extends AbstractSlashCommand implements ICommand {
    private final static Logger logger = LoggerFactory.getLogger(RemindersCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var user = ctx.getAuthor();
        final var args = ctx.getArgs();
        final String prefix = new GuildConfig().getPrefix(guild.getIdLong());

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
                default -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_ARGS
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

    private MessageEmbed handleAdd(Guild guild, User user, String reminder, String time, @Nullable Long channelID) {
        RemindersConfig remindersConfig = new RemindersConfig();

        channelID = channelID == null ? -1L : channelID;

        if (remindersConfig.channelIsBanned(guild.getIdLong(), channelID))
            return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                            RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                            RobertifyLocaleMessage.ReminderMessages.CANNOT_SET_BANNED_REMINDER_CHANNEL,
                            Pair.of("{channel}", GeneralUtils.toMention(guild, channelID, GeneralUtils.Mentioner.CHANNEL))
                    )
                    .build();

        TextChannel channel = guild.getTextChannelById(channelID);
        Member selfMember = guild.getSelfMember();

        if (channel != null)
            if (!selfMember.hasPermission(channel, net.dv8tion.jda.api.Permission.MESSAGE_WRITE))
                return RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, "I do not have enough permissions to send your reminder in: " + channel.getAsMention())
                        .build();

        long timeInMillis = 0;

        try {
            timeInMillis = timeToMillis(time);
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
                guild.getIdLong(),
                user.getIdLong(),
                reminder,
                channelID,
                timeInMillis
        );

        ReminderScheduler.getInstance()
                .scheduleReminder(
                        user.getIdLong(),
                        guild.getIdLong(),
                        channelID,
                        timeInMillis,
                        reminder,
                        remindersConfig.getReminders(guild.getIdLong(), user.getIdLong()).size()-1
                );

        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_ADDED).build();
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

        msg.replyEmbeds(handleRemove(guild, msg.getAuthor(), id-1)).queue();
    }

    private MessageEmbed handleRemove(Guild guild, User user, int id) {
        final var config = new RemindersConfig();

        List<Reminder> reminders = config.getReminders(guild.getIdLong(), user.getIdLong());

        if (reminders.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS)
                    .build();

        if (id < 0 || id > reminders.size()-1)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.INVALID_REMINDER_ID)
                    .build();

        config.removeReminder(guild.getIdLong(), user.getIdLong(), id);

        ReminderScheduler.getInstance()
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
        final var config = new RemindersConfig();

        try {
            config.clearReminders(guild.getIdLong(), user.getIdLong());
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
        RemindersConfig config = new RemindersConfig();

        if (!config.userHasReminders(guild.getIdLong(), user.getIdLong()))
            return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                    RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                    RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS
            ).build();

        final var reminders = config.getReminders(guild.getIdLong(), user.getIdLong());

        if (reminders.isEmpty())
            return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                    RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                    RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS
            ).build();

        final var sb = new StringBuilder();

        for (int i = 0; i < reminders.size(); i++) {
            Reminder reminder = reminders.get(i);
            sb.append("**").append(i + 1).append(".** - ")
                    .append(reminder.getReminder()).append(" `@ ")
                    .append(GeneralUtils.formatTime(reminder.getReminderTime(), "HH:mm"))
                    .append(" EDT`")
                    .append("\n");
        }

        return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE,
                sb.toString()
        ).build();
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

                msg.replyEmbeds(handleChannelEdit(guild, msg.getAuthor(), id-1, channel.getIdLong()))
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

                msg.replyEmbeds(handleTimeEdit(guild, msg.getAuthor(), (Integer.parseInt(reminderIDStr))-1, timeStr))
                        .queue();
            }
        }
    }

    private MessageEmbed handleChannelEdit(Guild guild, User user, int id, Long channelID) {
        final var config = new RemindersConfig();

        if (!config.userHasReminders(guild.getIdLong(), user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS).build();

        try {
            List<Reminder> reminders = config.getReminders(guild.getIdLong(), user.getIdLong());

            if (id < 0 || id > reminders.size())
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDER_WITH_ID).build();

            config.editReminderChannel(guild.getIdLong(), user.getIdLong(), id, channelID);

            Reminder reminder = reminders.get(id);

            ReminderScheduler.getInstance()
                    .editReminder(guild.getIdLong(), channelID, reminder.getUserId(), reminder.getId(), reminder.getReminderTime(), reminder.getReminder());

            if (channelID == -1L)
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_REMOVED, Pair.of("{id}", String.valueOf(id+1))).build();
            else
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_CHANNEL_CHANGED,
                        Pair.of("{id}", String.valueOf(id+1)),
                        Pair.of("{channel}", GeneralUtils.toMention(guild, channelID, GeneralUtils.Mentioner.CHANNEL))
                ).build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS).build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR).build();
        }
    }

    private MessageEmbed handleTimeEdit(Guild guild, User user, int id, String timeUnparsed) {
        long timeInMillis = 0;

        try {
            timeInMillis = timeToMillis(timeUnparsed);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("minute")) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_MINUTE).build();
            } else if (e.getMessage().contains("hour")) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INVALID_HOUR).build();
            } else if (e.getMessage().contains("time")) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_INVALID_TIME_FORMAT).build();
            }
        }

        final var config = new RemindersConfig();

        if (!config.userHasReminders(guild.getIdLong(), user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.NO_REMINDERS)
                    .build();

        List<Reminder> reminders = config.getReminders(guild.getIdLong(), user.getIdLong());

        if (id < 0 || id >= reminders.size())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.MISSING_REMINDER_ID)
                    .build();

        try {
            config.editReminderTime(guild.getIdLong(), user.getIdLong(), id, timeInMillis);

            Reminder reminder = reminders.get(id);

            ReminderScheduler.getInstance()
                    .editReminder(guild.getIdLong(), reminder.getChannelID(), reminder.getUserId(), reminder.getId(), timeInMillis, reminder.getReminder());
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_TIME_CHANGED,
                        Pair.of("{time}", timeUnparsed),
                        Pair.of("{id}", String.valueOf(id+1))
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

        if (Pattern.matches("^\\d{1,2}:\\d{1,2}(AM|PM)$", time)) {
            String[] split = time.split(":");
            final int hour = Integer.parseInt(split[0]);

            if (hour < 0 || hour > 12)
                throw new IllegalArgumentException("Invalid hour");

            final int minute = Integer.parseInt(GeneralUtils.getDigitsOnly(split[1]));

            if (minute < 0 || minute > 59)
                throw new IllegalArgumentException("Invalid minute");

            final String meridiemIndicator = GeneralUtils.removeAllDigits(split[1]);

            timeInMillis += TimeUnit.MINUTES.toMillis(minute);

            switch (meridiemIndicator.toLowerCase()) {
                case "am" -> timeInMillis += TimeUnit.HOURS.toMillis(hour);
                case "pm" -> timeInMillis += TimeUnit.HOURS.toMillis(hour + 12L);
            }
        } else if (Pattern.matches("^\\d{1,2}:\\d{1,2}$", time)) {
            String[] split = time.split(":");
            final int hour = Integer.parseInt(split[0]);

            if (hour < 0 || hour > 24)
                throw new IllegalArgumentException("Invalid hour");

            final int minute = Integer.parseInt(GeneralUtils.getDigitsOnly(split[1]));

            if (minute < 0 || minute > 59)
                throw new IllegalArgumentException("Invalid minute");


            timeInMillis += TimeUnit.HOURS.toMillis(hour);
            timeInMillis += TimeUnit.MINUTES.toMillis(minute);
        } else
            throw new IllegalArgumentException("Invalid time format!");

        return timeInMillis;
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
        final var config = new RemindersConfig();

        if (config.userIsBanned(guild.getIdLong(), idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.BanMessages.USER_ALREADY_BANNED).build();

        config.banUser(guild.getIdLong(), idToBan);
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
        final var config = new RemindersConfig();

        if (!config.userIsBanned(guild.getIdLong(), idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.USER_NOT_BANNED).build();

        config.unbanUser(guild.getIdLong(), idToBan);
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
        final var config = new RemindersConfig();

        if (config.channelIsBanned(guild.getIdLong(), idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_CHANNEL_ALREADY_BANNED).build();

        config.banChannel(guild.getIdLong(), idToBan);
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
        final var config = new RemindersConfig();

        if (!config.channelIsBanned(guild.getIdLong(), idToBan))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ReminderMessages.REMINDER_CHANNEL_NOT_BANNED).build();

        config.unbanChannel(guild.getIdLong(), idToBan);
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
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`" +
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
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        String[] split = event.getCommandPath().split("/");
        List<OptionMapping> options = event.getOptions();

        final Guild guild = event.getGuild();
        final Member member = event.getMember();
        final User eventUser = event.getUser();
        switch (split[1]) {
            case "add" -> {
                String time = options.get(0).getAsString();
                String reminder = options.get(1).getAsString();

                MessageChannel channel = null;
                if (options.size() == 3)
                    channel = options.get(2).getAsMessageChannel();

                event.replyEmbeds(handleAdd(guild, eventUser, reminder, time, channel != null ? channel.getIdLong() : -1L))
                        .setEphemeral(true)
                        .queue();
            }
            case "remove" -> {
                int id = (int)options.get(0).getAsLong();

                event.replyEmbeds(handleRemove(guild, eventUser, id-1))
                        .setEphemeral(true)
                        .queue();
            }
            case "edit" -> {
                switch (split[2]) {
                    case "channel" -> {
                        int id = (int)options.get(0).getAsLong();

                        MessageChannel channel = null;
                        if (options.size() == 2)
                           channel = options.get(1).getAsMessageChannel();

                        event.replyEmbeds(handleChannelEdit(guild, eventUser, id-1, channel != null ? channel.getIdLong() : -1L))
                                .setEphemeral(true)
                                .queue();
                    }
                    case "time" -> {
                        int id = (int)options.get(0).getAsLong();
                        String time = options.get(1).getAsString();

                        event.replyEmbeds(handleTimeEdit(guild, eventUser, id-1, time))
                                .setEphemeral(true)
                                .queue();
                    }
                }
            }
            case "clear" -> {
                event.replyEmbeds(handleClear(guild, eventUser))
                        .setEphemeral(true)
                        .queue();
            }
            case "list" -> {
                event.replyEmbeds(handleList(guild, eventUser))
                        .setEphemeral(true)
                        .queue();
            }
            case "ban" -> {
                if (!GeneralUtils.hasPerms(guild, member, Permission.ROBERTIFY_ADMIN)) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, BotConstants.getInsufficientPermsMessage(guild, Permission.ROBERTIFY_ADMIN)).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                switch (split[2]) {
                    case "channel" -> {
                        final var channel = options.get(0).getAsMessageChannel();

                        if (channel == null) {
                            event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, RobertifyLocaleMessage.GeneralMessages.MUST_PROVIDE_VALID_CHANNEL).build())
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        }

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
                        final var channel = options.get(0).getAsMessageChannel();

                        if (channel == null) {
                            event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.ReminderMessages.REMINDERS_EMBED_TITLE, RobertifyLocaleMessage.GeneralMessages.MUST_PROVIDE_VALID_CHANNEL).build())
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        }

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
}
