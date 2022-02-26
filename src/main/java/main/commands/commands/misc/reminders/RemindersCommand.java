package main.commands.commands.misc.reminders;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.reminders.Reminder;
import main.utils.json.reminders.RemindersConfig;
import net.dv8tion.jda.api.entities.*;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RemindersCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var user = ctx.getAuthor();
        final var args = ctx.getArgs();
        final String prefix = new GuildConfig().getPrefix(guild.getIdLong());

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide arguments!"
                            + getUsages(prefix)).build())
                    .queue();
        } else {
            switch (args.get(0).toLowerCase()) {
                case "add" -> add(msg, args);
                case "remove" -> remove(msg, args);
                case "clear" -> clear(msg);
                case "list" -> list(msg);
                case "edit" -> edit(msg, args);
                case "banuser" -> banUser();
                case "unbanuser" -> unbanUser();
                case "banchannel" -> banChannel();
                case "unbanchannel" -> unbanChannel();
                default -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Invalid arguments!"
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
        long timeInMillis = 0;

        try {
            timeInMillis = timeToMillis(time);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("minute")) {
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid minute!").build();
            } else if (e.getMessage().contains("hour")) {
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid hour!").build();
            } else if (e.getMessage().contains("time")) {
                return RobertifyEmbedUtils.embedMessage(guild, """
                    Invalid time format!
                    
                    **Correct Format** : `00:00AM/PM` **__OR__** `00:00` *(24-hour format)*
                    """).build();
            }
        }

        RemindersConfig remindersConfig = new RemindersConfig();

        remindersConfig.addReminder(
                guild.getIdLong(),
                user.getIdLong(),
                reminder,
                channelID == null ? -1 : channelID,
                timeInMillis
        );

        return RobertifyEmbedUtils.embedMessage(guild, "Successfully added your reminder!").build();
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
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any reminders!")
                    .build();

        if (id < 0 || id > reminders.size()-1)
            return RobertifyEmbedUtils.embedMessage(guild, "Invalid reminder ID!")
                    .build();

        config.removeReminder(guild.getIdLong(), user.getIdLong(), id);

        return RobertifyEmbedUtils.embedMessage(guild, "You have removed:\n\n```" + reminders.get(id).getReminder() + "```")
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
            return RobertifyEmbedUtils.embedMessage(guild, "You have successfully cleared all reminders!")
                    .build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any reminders!").build();
        } catch (Exception e) {
            return RobertifyEmbedUtils.embedMessage(guild, """
                    An unexpected error occurred!

                    Please join our [support server](https://robertify.me/support) to report this to the developers!""").build();
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
                    "Reminders",
                    "You have no reminders!"
            ).build();

        final var reminders = config.getReminders(guild.getIdLong(), user.getIdLong());
        final var sb = new StringBuilder();

        for (int i = 0; i < reminders.size(); i++) {
            Reminder reminder = reminders.get(i);
            sb.append("**").append(i + 1).append(".** - ")
                    .append(reminder.getReminder()).append(" `@ ")
                    .append(GeneralUtils.formatTime(reminder.getReminderTime(), "HH:mm"))
                    .append("`")
                    .append("\n");
        }

        return RobertifyEmbedUtils.embedMessageWithTitle(guild,
                "Reminders",
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

                TextChannel channel = Robertify.api.getTextChannelById(GeneralUtils.getDigitsOnly(channelIDStr));

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
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any reminders!").build();

        try {
            List<Reminder> reminders = config.getReminders(guild.getIdLong(), user.getIdLong());

            if (id < 0 || id > reminders.size())
                return RobertifyEmbedUtils.embedMessage(guild, "There exists no such reminder with that ID!").build();

            config.editReminderChannel(guild.getIdLong(), user.getIdLong(), id, channelID);
            return RobertifyEmbedUtils.embedMessage(guild, "You have successfully changed the reminder channel for reminder " +
                    "`"+(id+1)+"` to: "
                    + GeneralUtils.toMention(channelID, GeneralUtils.Mentioner.CHANNEL)).build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any reminders!").build();
        } catch (Exception e) {
            e.printStackTrace();
            return RobertifyEmbedUtils.embedMessage(guild, """
                    An unexpected error occurred!

                    Please join our [support server](https://robertify.me/support) to report this to the developers!""").build();
        }
    }

    private MessageEmbed handleTimeEdit(Guild guild, User user, int id, String timeUnparsed) {
        long timeInMillis = 0;

        try {
            timeInMillis = timeToMillis(timeUnparsed);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("minute")) {
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid minute!").build();
            } else if (e.getMessage().contains("hour")) {
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid hour!").build();
            } else if (e.getMessage().contains("time")) {
                return RobertifyEmbedUtils.embedMessage(guild, """
                    Invalid time format!
                    
                    **Correct Format** : `00:00AM/PM` **__OR__** `00:00` *(24-hour format)*
                    """).build();
            }
        }

        final var config = new RemindersConfig();

        if (!config.userHasReminders(guild.getIdLong(), user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any reminders!")
                    .build();

        List<Reminder> reminders = config.getReminders(guild.getIdLong(), user.getIdLong());

        if (id < 0 || id >= reminders.size())
            return RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid reminder ID!")
                    .build();

        try {
            config.editReminderTime(guild.getIdLong(), user.getIdLong(), id, timeInMillis);
            return RobertifyEmbedUtils.embedMessage(guild, "You have successfully changed the time of reminder with ID `"+(id+1)+"` to: `"+timeUnparsed+"`")
                    .build();
        } catch (NullPointerException e) {
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have any reminders!")
                    .build();
        } catch (Exception e) {
            return RobertifyEmbedUtils.embedMessage(guild, """
                    An unexpected error occurred!

                    Please join our [support server](https://robertify.me/support) to report this to the developers!""").build();
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

    private void banUser() {

    }

    private void unbanUser() {

    }

    private void banChannel() {

    }

    private void unbanChannel() {

    }

    @Override
    public String getName() {
        return "reminders";
    }

    @Override
    public List<String> getAliases() {
        return List.of("reminder", "reminders");
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
        return null;
    }
}
