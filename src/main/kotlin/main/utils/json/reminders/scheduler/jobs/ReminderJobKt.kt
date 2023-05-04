package main.utils.json.reminders.scheduler.jobs

import main.constants.ToggleKt
import main.main.RobertifyKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.json.reminders.RemindersConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.ReminderMessages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import org.quartz.Job
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import java.time.Instant

class ReminderJobKt : Job {
    override fun execute(context: JobExecutionContext) {
        val dataMap: JobDataMap = context.jobDetail.jobDataMap
        val guildID = dataMap.getLong("guild")
        val destination = dataMap.getLong("destination")
        val user = dataMap.getLong("user")
        val reminder = dataMap.getString("reminder")

        val guild = RobertifyKt.shardManager.getGuildById(guildID) ?: return

        if (destination == -1L) {
            dmReminder(guild, user, reminder)
            return
        }

        val remindersConfig = RemindersConfigKt(guild)
        if (remindersConfig.userIsBanned(user)) {
            dmReminder(guild, user, reminder)
            return
        }

        val channel = guild.getTextChannelById(destination)

        if (channel == null) {
            dmReminder(guild, user, reminder)
            return
        }

        if (!guild.selfMember.hasPermission(channel, Permission.MESSAGE_SEND)) {
            dmReminder(guild, user, reminder)
            return
        }

        if (!remindersConfig.channelIsBanned(channel.idLong)) {
            dmReminder(guild, user, reminder)
            return
        }

        if (!TogglesConfigKt(guild).getToggle(ToggleKt.REMINDERS)) return

        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        channel.sendMessage(
            localeManager.getMessage(
                ReminderMessages.REMINDER_SEND,
                Pair(
                    "{user}",
                    GeneralUtilsKt.toMention(guild, user, GeneralUtilsKt.Mentioner.USER)
                )
            )
        )
            .setEmbeds(
                RobertifyEmbedUtilsKt.embedMessageWithTitle(
                    guild,
                    localeManager.getMessage(ReminderMessages.REMINDERS_EMBED_TITLE),
                    reminder
                )
                    .setTimestamp(Instant.now())
                    .build()
            )
            .queue()
    }

    private fun dmReminder(guild: Guild, user: Long, reminder: String) {
        val localeManager = LocaleManagerKt.getLocaleManager(guild)

        GeneralUtilsKt.dmUser(
            user,
            RobertifyEmbedUtilsKt.embedMessageWithTitle(
                guild,
                localeManager.getMessage(ReminderMessages.REMINDERS_EMBED_TITLE),
                reminder
            )
                .setFooter(
                    localeManager.getMessage(
                        ReminderMessages.REMINDER_FROM,
                        Pair("{server}", guild.name)
                    )
                )
                .setTimestamp(Instant.now())
                .build()
        )
    }
}