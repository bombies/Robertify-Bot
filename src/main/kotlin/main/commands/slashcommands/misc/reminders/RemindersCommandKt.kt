package main.commands.slashcommands.misc.reminders

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.BotConstantsKt
import main.constants.InteractionLimitsKt
import main.constants.RobertifyPermissionKt
import main.constants.ToggleKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.coerceAtMost
import main.utils.GeneralUtilsKt.digits
import main.utils.GeneralUtilsKt.hasPermissions
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
import main.utils.locale.messages.BanMessages
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.ReminderMessages
import main.utils.locale.messages.UnbanMessages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.time.LocalDate
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
                                description = "The new channel to send the reminder in.",
                                required = false
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

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        if (!TogglesConfigKt(guild)[ToggleKt.REMINDERS])
            return event.replyEmbed(guild, GeneralMessages.DISABLED_FEATURE).queue()

        val (_, primaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (primaryCommand) {
            "add" -> handleAdd(event)
            "remove" -> handleRemove(event)
            "edit" -> handleEdit(event)
            "clear" -> handleClear(event)
            "list" -> handleList(event)
            "ban" -> handleBan(event)
            "unban" -> handleUnban(event)
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

        val (timeInMillis, hour, minute) = handleTimeParsing(event, time) ?: return

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
                hour = hour.toInt(),
                minute = minute.toInt(),
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

        val id = event.getRequiredOption("id").asInt - 1
        if (id < 0 || id >= reminders.size)
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

    private fun handleClear(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val config = RemindersConfigKt(guild)

        return try {
            config - event.user.idLong
            event.replyEmbed(guild, ReminderMessages.REMINDERS_CLEARED).setEphemeral(true).queue()
        } catch (e: NullPointerException) {
            event.replyEmbed(guild, ReminderMessages.NO_REMINDERS).setEphemeral(true).queue()
        } catch (e: Exception) {
            event.replyEmbed(guild, GeneralMessages.UNEXPECTED_ERROR).setEphemeral(true).queue()
        }
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val user = event.user
        val config = RemindersConfigKt(guild)

        if (!config.userHasReminders(user.idLong))
            return event.replyEmbed(guild, ReminderMessages.NO_REMINDERS).setEphemeral(true).queue()

        val reminders = config[user.idLong]
        if (reminders.isNullOrEmpty())
            return event.replyEmbed(guild, ReminderMessages.NO_REMINDERS).setEphemeral(true).queue()

        val listString = reminders.mapIndexed { i, reminder ->
            val nextTimeStamp = getNextUnixTimestamp(reminder)
            "**${i + 1}.** - ${reminder.reminder} <t:$nextTimeStamp:t> (<t:$nextTimeStamp:R>)"
        }.joinToString("\n")

        event.replyEmbed(guild, listString).setEphemeral(true).queue()
    }

    private fun handleEdit(event: SlashCommandInteractionEvent) {
        val (_, _, secondaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (secondaryCommand) {
            "channel" -> handleChannelEdit(event)
            "time" -> handleTimeEdit(event)
        }
    }

    private fun handleChannelEdit(event: SlashCommandInteractionEvent) {
        val id = event.getRequiredOption("id").asInt - 1
        val channel = event.getOption("channel")?.asChannel?.asGuildMessageChannel()
        val guild = event.guild!!
        val config = RemindersConfigKt(guild)
        val user = event.member!!

        if (!config.userHasReminders(user.idLong))
            return event.replyEmbed(guild, ReminderMessages.NO_REMINDERS).setEphemeral(true).queue()

        try {
            val reminders = config.getReminders(user.idLong)
            if (reminders.isNullOrEmpty())
                return event.replyEmbed(guild, ReminderMessages.NO_REMINDERS).setEphemeral(true).queue()

            if (id < 0 || id > reminders.size)
                return event.replyEmbed(guild, ReminderMessages.NO_REMINDER_WITH_ID).setEphemeral(true).queue()

            val selfMember = guild.selfMember

            if (channel != null) {
                if (!selfMember.hasPermission(channel, Permission.MESSAGE_SEND))
                    return event.replyEmbed(guild, ReminderMessages.REMINDER_INSUFFICIENT_PERMISSIONS)
                        .setEphemeral(true)
                        .queue()
                if (!user.hasPermission(channel, Permission.MESSAGE_SEND))
                    return event.replyEmbed(guild, ReminderMessages.REMINDER_INSUFFICIENT_USER_PERMISSIONS)
                        .setEphemeral(true)
                        .queue()
            }


            config.editReminderChannel(user.idLong, id, channel?.idLong ?: -1)
            val reminder = reminders[id]

            ReminderSchedulerKt(guild)
                .editReminder(
                    channelId = channel?.idLong ?: -1L,
                    user = user.idLong,
                    newHour = reminder.hour,
                    newMinute = reminder.minute,
                    reminder = reminder.reminder,
                    timeZone = reminder.timezone.id,
                    reminderId = reminder.id
                )

            return if (channel == null)
                event.replyEmbed(guild, ReminderMessages.REMINDER_CHANNEL_REMOVED, Pair("{id}", (id + 1).toString()))
                    .setEphemeral(true)
                    .queue()
            else event.replyEmbed(
                guild,
                ReminderMessages.REMINDER_CHANNEL_CHANGED,
                Pair("{id}", (id + 1).toString()),
                Pair("{channel}", channel.asMention)
            ).setEphemeral(true).queue()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            return event.replyEmbed(guild, GeneralMessages.UNEXPECTED_ERROR).setEphemeral(true).queue()
        }
    }

    private fun handleTimeEdit(event: SlashCommandInteractionEvent) {
        val id = event.getRequiredOption("id").asInt - 1
        val time = event.getRequiredOption("time").asString
        val guild = event.guild!!
        val config = RemindersConfigKt(guild)
        val user = event.user

        val (timeInMillis, hour, minute) = handleTimeParsing(event, time) ?: return

        if (!config.userHasReminders(user.idLong))
            return event.replyEmbed(guild, ReminderMessages.NO_REMINDERS).setEphemeral(true).queue()

        val reminders = config[user.idLong]
        if (reminders.isNullOrEmpty())
            return event.replyEmbed(guild, ReminderMessages.NO_REMINDERS).setEphemeral(true).queue()

        if (id < 0 || id >= reminders.size)
            return event.replyEmbed(guild, ReminderMessages.INVALID_REMINDER_ID).setEphemeral(true).queue()

        return try {
            config.editReminderTime(user.idLong, id, timeInMillis)
            val reminder = reminders[id]
            ReminderSchedulerKt(guild)
                .editReminder(
                    channelId = reminder.channelId,
                    user = reminder.userId,
                    reminderId = reminder.id,
                    newHour = hour.toInt(),
                    newMinute = minute.toInt(),
                    reminder = reminder.reminder,
                    timeZone = reminder.timezone.id
                )
            event.replyEmbed(
                guild,
                ReminderMessages.REMINDER_TIME_CHANGED,
                Pair("{time}", time),
                Pair("{id}", (id + 1).toString())
            )
                .setEphemeral(true)
                .queue()
        } catch (e: Exception) {
            logger.error("An unexpected error occurred", e)
            event.replyEmbed(guild, GeneralMessages.UNEXPECTED_ERROR).setEphemeral(true).queue()
        }
    }

    private fun handleBan(event: SlashCommandInteractionEvent) {
        handleGenericBanAction(
            event = event,
            handleChannel = { handleChannelBan(it) },
            handleUser = { handleUserBan(it) }
        )
    }

    private fun handleChannelBan(event: SlashCommandInteractionEvent) {
        val channel = event.getRequiredOption("channel").asChannel.asGuildMessageChannel()
        val guild = event.guild!!
        val config = RemindersConfigKt(guild)

        if (config.channelIsBanned(channel.idLong))
            return event.replyEmbed(guild, ReminderMessages.REMINDER_CHANNEL_ALREADY_BANNED).setEphemeral(true).queue()

        config.banChannel(channel.idLong)
        event.replyEmbed(guild, BanMessages.USER_PERM_BANNED_RESPONSE, Pair("{user}", channel.asMention))
            .setEphemeral(true)
            .queue()
    }

    private fun handleUserBan(event: SlashCommandInteractionEvent) {
        val user = event.getRequiredOption("user").asUser
        val guild = event.guild!!
        val config = RemindersConfigKt(guild)

        if (config.userIsBanned(user.idLong))
            return event.replyEmbed(guild, BanMessages.USER_ALREADY_BANNED).setEphemeral(true).queue()

        config.banUser(user.idLong)
        event.replyEmbed(guild, BanMessages.USER_PERM_BANNED_RESPONSE, Pair("{user}", user.asMention))
            .setEphemeral(true)
            .queue()
    }

    private fun handleUnban(event: SlashCommandInteractionEvent) {
        handleGenericBanAction(
            event = event,
            handleChannel = { handleChannelUnban(it) },
            handleUser = { handleUserUnban(it) }
        )
    }

    private fun handleChannelUnban(event: SlashCommandInteractionEvent) {
        val channel = event.getRequiredOption("channel").asChannel.asGuildMessageChannel()
        val guild = event.guild!!
        val config = RemindersConfigKt(guild)

        if (!config.channelIsBanned(channel.idLong))
            return event.replyEmbed(guild, ReminderMessages.REMINDER_CHANNEL_NOT_BANNED).setEphemeral(true).queue()

        config.unbanChannel(channel.idLong)
        event.replyEmbed(guild, UnbanMessages.USER_UNBANNED_RESPONSE, Pair("{user}", channel.asMention))
            .setEphemeral(true)
            .queue()
    }

    private fun handleUserUnban(event: SlashCommandInteractionEvent) {
        val user = event.getRequiredOption("user").asUser
        val guild = event.guild!!
        val config = RemindersConfigKt(guild)

        if (!config.userIsBanned(user.idLong))
            return event.replyEmbed(guild, UnbanMessages.USER_NOT_BANNED).setEphemeral(true).queue()

        config.unbanUser(user.idLong)
        event.replyEmbed(guild, UnbanMessages.USER_UNBANNED_RESPONSE, Pair("{user}", user.asMention))
            .setEphemeral(true)
            .queue()
    }

    private inline fun handleGenericBanAction(
        event: SlashCommandInteractionEvent,
        handleChannel: (event: SlashCommandInteractionEvent) -> Unit,
        handleUser: (event: SlashCommandInteractionEvent) -> Unit
    ) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermissionKt.ROBERTIFY_ADMIN))
            return event.replyEmbed(
                guild,
                BotConstantsKt.getInsufficientPermsMessage(guild, RobertifyPermissionKt.ROBERTIFY_ADMIN)
            ).setEphemeral(true).queue()

        val (_, _, secondaryCommand) = event.fullCommandName.split("\\s".toRegex())
        when (secondaryCommand) {
            "channel" -> handleChannel(event)
            "user" -> handleUser(event)
        }
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
                    reminder.reminder.length.coerceAtMost(InteractionLimitsKt.COMMAND_OPTION_CHOICE_LENGTH)
                ), reminder.id.toLong() + 1
            )
        }.coerceAtMost(25))
            .queue()
    }

    private fun handleTimeParsing(event: SlashCommandInteractionEvent, time: String): List<Long>? {
        val guild = event.guild!!
        return try {
            val timeInMillis = timeToMillis(time)
            val hour = extractTime(time, DurationUnit.HOURS)
            val minute = extractTime(time, DurationUnit.MINUTES)
            return listOf(timeInMillis, hour.toLong(), minute.toLong())
        } catch (e: IllegalArgumentException) {
            when {
                e.message?.contains("minute") == true ->
                    event.replyEmbed(guild, GeneralMessages.INVALID_MINUTE).setEphemeral(true).queue()

                e.message?.contains("hour") == true ->
                    event.replyEmbed(guild, GeneralMessages.INVALID_HOUR).setEphemeral(true).queue()

                e.message?.contains("time") == true ->
                    event.replyEmbed(guild, ReminderMessages.REMINDER_INVALID_TIME_FORMAT).setEphemeral(true)
                        .queue()

                else -> throw e
            }
            null
        }
    }

    private fun getNextUnixTimestamp(reminder: ReminderKt): Long {
        val todaysTime = LocalDate.now().atStartOfDay(reminder.timezone.toZoneId())
            .toEpochSecond() + reminder.reminderTime.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds
        val tomorrowsTime = todaysTime + 86400L
        return if (todaysTime < System.currentTimeMillis()
                .toDuration(DurationUnit.MILLISECONDS).inWholeSeconds
        ) tomorrowsTime else todaysTime
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