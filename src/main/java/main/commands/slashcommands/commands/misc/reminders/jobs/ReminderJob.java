package main.commands.slashcommands.commands.misc.reminders.jobs;

import main.constants.Toggles;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.reminders.RemindersConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class ReminderJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final var dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        final var guildID = dataMap.getLong("guild");
        final var destination = dataMap.getLong("destination");
        final var user = dataMap.getLong("user");
        final var reminder = dataMap.getString("reminder");

        final Guild guild = Robertify.shardManager.getGuildById(guildID);

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
}