package main.commands.commands.misc.reminders;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.reminders.Reminder;
import main.utils.json.reminders.RemindersConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

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
                case "remove" -> remove();
                case "clear" -> clear();
                case "list" -> list(msg);
                case "edit" -> edit();
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

        if (Pattern.matches("^\\d{1,2}:\\d{1,2}(AM|PM)$", time)) {
            String[] split = time.split(":");
            final int hour = Integer.parseInt(split[0]);

            if (hour < 0 || hour > 12)
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid hour!").build();

            final int minute = Integer.parseInt(GeneralUtils.getDigitsOnly(split[1]));

            if (minute < 0 || minute > 59)
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid minute!").build();

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
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid hour!").build();

            final int minute = Integer.parseInt(GeneralUtils.getDigitsOnly(split[1]));

            if (minute < 0 || minute > 59)
                return RobertifyEmbedUtils.embedMessage(guild, "Invalid minute!").build();

            timeInMillis += TimeUnit.HOURS.toMillis(hour);
            timeInMillis += TimeUnit.MINUTES.toMillis(minute);
        } else
            return RobertifyEmbedUtils.embedMessage(guild, """
                    Invalid time format!
                    
                    **Correct Format** : `00:00AM/PM` **__OR__** `00:00` *(24-hour format)*
                    """).build();

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

    private void remove() {

    }

    private void clear() {

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

    private void edit() {

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
                "`" + prefix + "reminders edit time <(00:00AM/PM)|00:00>`\n" +
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
