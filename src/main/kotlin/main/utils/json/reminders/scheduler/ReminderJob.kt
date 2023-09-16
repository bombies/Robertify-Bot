package main.utils.json.reminders.scheduler

import kotlinx.coroutines.runBlocking
import main.constants.Toggle
import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.json.reminders.RemindersConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.ReminderMessages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import org.quartz.Job
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import java.time.Instant

class ReminderJob : Job {
    override fun execute(context: JobExecutionContext) {
        runBlocking {
            val dataMap: JobDataMap = context.jobDetail.jobDataMap
            val guildID = dataMap.getLong("guild")
            val destination = dataMap.getLong("destination")
            val user = dataMap.getLong("user")
            val reminder = dataMap.getString("reminder")

            val guild = Robertify.shardManager.getGuildById(guildID) ?: return@runBlocking

            if (destination == -1L) {
                dmReminder(guild, user, reminder)
                return@runBlocking
            }

            val remindersConfig = RemindersConfig(guild)
            if (remindersConfig.userIsBanned(user)) {
                dmReminder(guild, user, reminder)
                return@runBlocking
            }

            val channel = guild.getTextChannelById(destination)

            if (channel == null) {
                dmReminder(guild, user, reminder)
                return@runBlocking
            }

            if (!guild.selfMember.hasPermission(channel, Permission.MESSAGE_SEND)) {
                dmReminder(guild, user, reminder)
                return@runBlocking
            }

            if (remindersConfig.channelIsBanned(channel.idLong)) {
                dmReminder(guild, user, reminder)
                return@runBlocking
            }

            if (!TogglesConfig(guild).getToggle(Toggle.REMINDERS)) return@runBlocking

            val localeManager = LocaleManager[guild]
            channel.sendMessage(
                localeManager.getMessage(
                    ReminderMessages.REMINDER_SEND,
                    Pair(
                        "{user}",
                        GeneralUtils.toMention(guild, user, GeneralUtils.Mentioner.USER)
                    )
                )
            )
                .setEmbeds(
                    RobertifyEmbedUtils.embedMessageWithTitle(
                        guild,
                        localeManager.getMessage(ReminderMessages.REMINDERS_EMBED_TITLE),
                        reminder
                    )
                        .setTimestamp(Instant.now())
                        .build()
                )
                .queue()
        }
    }

    private suspend fun dmReminder(guild: Guild, user: Long, reminder: String) {
        val localeManager = LocaleManager[guild]

        GeneralUtils.dmUser(
            user,
            RobertifyEmbedUtils.embedMessageWithTitle(
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