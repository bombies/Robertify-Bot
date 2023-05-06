package main.commands.slashcommands.misc.reminders

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.ToggleKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.digits
import main.utils.GeneralUtilsKt.stripDigits
import main.utils.GeneralUtilsKt.toMention
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandGroupKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.reminders.ReminderKt
import main.utils.json.reminders.RemindersConfigKt
import main.utils.json.reminders.scheduler.ReminderSchedulerKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.ReminderMessages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.TimeZone
import kotlin.IllegalArgumentException
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class RemindersCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "reminders",
        description = "Set your reminders.",
        subcommands = listOf(
            SubCommandKt(
                name = "add",
                description = "Add a reminder.",
                options = listOf(
                    CommandOptionKt(
                        name = "time",
                        description = "The time to remind you at daily."
                    ),
                    CommandOptionKt(
                        name = "reminder",
                        description = "What you want to be reminded of."
                    ),
                    CommandOptionKt(
                        type = OptionType.CHANNEL,
                        channelTypes = listOf(ChannelType.TEXT),
                        name = "channel",
                        description = "The channel to send the reminder in.",
                        required = false
                    ),
                    CommandOptionKt(
                        name = "timezone",
                        description = "The timezone to send the reminder in",
                        required = false,
                        choices = RemindersConfigKt.validTimeZones
                    )
                ),
            ),
            SubCommandKt(
                name = "list",
                description = "List all your reminders."
            ),
            SubCommandKt(
                name = "remove",
                description = "Remove a specific reminder",
                options = listOf(
                    CommandOptionKt(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The ID of the reminder to remove.",
                        autoComplete = true
                    )
                )
            ),
            SubCommandKt(
                name = "clear",
                description = "Remove all your reminders"
            )
        ),
        subCommandGroups = listOf(
            SubCommandGroupKt(
                name = "edit",
                description = "Edit your reminders.",
                subCommands = listOf(
                    SubCommandKt(
                        name = "channel",
                        description = "Edit the channel a specific reminder gets sent in.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.INTEGER,
                                name = "id",
                                description = "The ID of the reminder to edit.",
                                autoComplete = true
                            ),
                            CommandOptionKt(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The new channel to send the reminder in."
                            )
                        )
                    ),
                    SubCommandKt(
                        name = "time",
                        description = "Edit the time a specific reminder gets sent at.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.INTEGER,
                                name = "id",
                                description = "The ID of the reminder to edit.",
                                autoComplete = true
                            ),
                            CommandOptionKt(
                                name = "time",
                                description = "Thew new time to send the reminder at."
                            )
                        )
                    )
                )
            ),
            SubCommandGroupKt(
                name = "ban",
                description = "Ban either users or channels.",
                subCommands = listOf(
                    SubCommandKt(
                        name = "channel",
                        description = "Ban a specific channel from being used.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The channel to ban"
                            )
                        )
                    ),
                    SubCommandKt(
                        name = "user",
                        description = "Ban a specific user from receiving reminders in this server.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.USER,
                                name = "user",
                                description = "The user to ban."
                            )
                        )
                    )
                )
            ),
            SubCommandGroupKt(
                name = "unban",
                description = "Unban either users or channels.",
                subCommands = listOf(
                    SubCommandKt(
                        name = "channel",
                        description = "Unban a specific channel.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The channel to unban"
                            )
                        )
                    ),
                    SubCommandKt(
                        name = "user",
                        description = "Unban a specific user.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.USER,
                                name = "user",
                                description = "The user to unban."
                            )
                        )
                    )
                )
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        if (!TogglesConfigKt(guild)[ToggleKt.REMINDERS])
            return event.replyEmbed(guild, GeneralMessages.DISABLED_FEATURE).queue()

        val (_, primaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (primaryCommand) {
            "add" -> handleAdd(event)
            "remove" -> handleRemove(event)
            "edit" -> {}
            "clear" -> {}
            "list" -> {}
            "ban" -> {}
            "unban" -> {}
        }
    }

    private fun handleAdd(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val time = event.getRequiredOption("time").asString
        val reminder = event.getRequiredOption("reminder").asString
        val channel = event.getOption("channel")?.asChannel?.asGuildMessageChannel()
        val timeZone = event.getOption("timezone")?.asString
        val config = RemindersConfigKt(guild)

        val channelId = channel?.idLong ?: -1L
        if (config.channelIsBanned(channelId))
            return event.replyEmbed(
                guild,
                ReminderMessages.CANNOT_SET_BANNED_REMINDER_CHANNEL,
                Pair("{channel}", channelId.toMention(GeneralUtilsKt.Mentioner.CHANNEL))
            )
                .setEphemeral(true)
                .queue()

        val selfMember = guild.selfMember
        val member = event.member!!

        if (channel != null) {
            if (!selfMember.hasPermission(channel, Permission.MESSAGE_SEND))
                return event.replyEmbed(
                    guild,
                    ReminderMessages.REMINDER_INSUFFICIENT_PERMISSIONS,
                    Pair("{channel}", channel.asMention)
                ).setEphemeral(true).queue()

            if (!member.hasPermission(channel, Permission.MESSAGE_SEND))
                return event.replyEmbed(
                    guild,
                    ReminderMessages.REMINDER_INSUFFICIENT_USER_PERMISSIONS,
                    Pair("{channel}", channel.asMention)
                ).setEphemeral(true).queue()
        }

        var timeInMillis = 0L
        var hour = 0;
        var minute = 0

        try {
            timeInMillis = timeToMillis(time)
            hour = extractTime(time, DurationUnit.HOURS)
            minute = extractTime(time, DurationUnit.MINUTES)
        } catch (e: IllegalArgumentException) {
            when {
                e.message?.contains("minute") == true ->
                    return event.replyEmbed(guild, GeneralMessages.INVALID_MINUTE).setEphemeral(true).queue()

                e.message?.contains("hour") == true ->
                    return event.replyEmbed(guild, GeneralMessages.INVALID_HOUR).setEphemeral(true).queue()

                e.message?.contains("time") == true ->
                    return event.replyEmbed(guild, ReminderMessages.REMINDER_INVALID_TIME_FORMAT).setEphemeral(true)
                        .queue()
            }
        }

        // Adds a reminder to the user
        config + ReminderKt(
            id = 0,
            userId = member.idLong,
            reminder = reminder,
            channelId = channelId,
            reminderTime = timeInMillis,
            _timezone = timeZone
        )

        ReminderSchedulerKt(guild)
            .scheduleReminder(
                user = member.idLong,
                destination = channelId,
                hour = hour,
                minute = minute,
                reminder = reminder,
                reminderId = config[member.idLong]!!.size - 1,
                timeZone = timeZone
            )
        event.replyEmbed(guild, ReminderMessages.REMINDER_ADDED).setEphemeral(true).queue()
    }

    private fun handleRemove(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val member = event.member!!
        val config = RemindersConfigKt(guild)
        val reminders = config[member.idLong]

        if (reminders.isNullOrEmpty())
            return event.replyEmbed(guild, ReminderMessages.NO_REMINDERS)
                .setEphemeral(true)
                .queue()

        val id = event.getRequiredOption("id").asInt
        if (id < 0 || id > reminders.size)
            return event.replyEmbed(guild, ReminderMessages.INVALID_REMINDER_ID)
                .setEphemeral(true)
                .queue()

        // Removes the reminder from the user
        config - Pair(member.idLong, id)

        ReminderSchedulerKt(guild).removeReminder(member.idLong, id)
        event.replyEmbed(guild, ReminderMessages.REMINDER_REMOVED, Pair("{reminder}", reminders[id].reminder))
            .setEphemeral(true)
            .queue()
    }

    override suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "reminders" && event.focusedOption.name != "id") return

        val guild = event.guild!!
        val reminders = RemindersConfigKt(guild)[event.user.idLong]
        if (reminders.isNullOrEmpty())
            return event.replyChoices().queue()

        event.replyChoices(reminders.map { reminder ->
            Command.Choice(
                reminder.reminder.substring(
                    0,
                    reminder.reminder.length.coerceAtMost(99)
                ), reminder.id.toLong()
            )
        })
            .queue()
    }

    private fun timeToMillis(time: String): Long {
        val timeSplit = splitTime(time)
        return timeSplit.first.toDuration(DurationUnit.HOURS).inWholeMilliseconds + timeSplit.second.toDuration(
            DurationUnit.MINUTES
        ).inWholeMilliseconds
    }

    private fun extractTime(time: String, duration: DurationUnit): Int {
        val timeSplit = splitTime(time)
        return when (duration) {
            DurationUnit.HOURS -> timeSplit.first
            DurationUnit.MINUTES -> timeSplit.second
            else -> throw IllegalArgumentException("Invalid time to extract!")
        }
    }

    private fun splitTime(time: String): Pair<Int, Int> {
        var hour = 0
        var minute = 0

        val handleInitialParsing: () -> List<String> = parser@{
            val (hourStr, minuteStr) = time.split(":")
            hour = hourStr.toInt()

            if (hour < 0 || hour > 12)
                throw IllegalArgumentException("Invalid hour")

            minute = minuteStr.digits().toInt()
            if (minute < 0 || minute > 59)
                throw IllegalArgumentException("Invalid minute")

            return@parser listOf(hourStr, minuteStr)
        }

        if (time.matches("^\\d{1,2}:\\d{1,2}(AM|PM|am|pm)$".toRegex())) {
            val (_, minuteStr) = handleInitialParsing()

            val meridiemIndicator = minuteStr.stripDigits()

            if (hour == 12 && meridiemIndicator.equals("am", ignoreCase = true))
                hour = 0
            else if (meridiemIndicator.equals("pm", ignoreCase = true))
                hour += 12
        } else if (time.matches("^\\d{1,2}:\\d{1,2}$".toRegex())) {
            handleInitialParsing()
        } else throw IllegalArgumentException("Invalid time format!")
        return Pair(hour, minute)
    }

    override val help: String
        get() = """
                **__Usages__**

                __General Commands__
                `/reminders add <(00:00AM/PM)|00:00> [#channel] <reminder>`
                `/reminders remove <id>`
                `/reminders clear`
                `/reminders list`
                `/reminders edit channel <ID> <#channel>`
                `/reminders edit time  <ID> <(00:00AM/PM)|00:00>`

                __Admin Commands__
                `/reminders ban channel <channel>`
                `/reminders unban channel <channel>`
                `/reminders ban user <user>`
                `/reminders unban user <user>`

                **NOTE**
                *<> - Required*
                *[] - Optional*
                """.trimIndent()
}