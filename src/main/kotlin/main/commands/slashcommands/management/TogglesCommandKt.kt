package main.commands.slashcommands.management

import dev.minn.jda.ktx.interactions.components.secondary
import dev.minn.jda.ktx.interactions.components.success
import main.commands.slashcommands.SlashCommandManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.commands.slashcommands.audio.SkipCommandKt
import main.constants.InteractionLimitsKt
import main.constants.RobertifyEmojiKt
import main.constants.ToggleKt
import main.utils.GeneralUtilsKt.coerceAtMost
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandGroupKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.RobertifyEmbedUtilsKt.Companion.addField
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.TogglesMessages
import main.utils.pagination.PaginationHandlerKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.components.ActionRow

class TogglesCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "toggles",
        description = "Toggle specific features on or off.",
        adminOnly = true,
        subcommands = listOf(
            SubCommandKt(
                name = "list",
                description = "List all toggles and their statuses."
            ),
            SubCommandKt(
                name = "switch",
                description = "Turn a specific toggle on or off.",
                options = listOf(
                    CommandOptionKt(
                        name = "toggle",
                        description = "The toggle to switch.",
                        choices = ToggleKt.toList()
                    )
                )
            )
        ),
        subCommandGroups = listOf(
            SubCommandGroupKt(
                name = "dj",
                description = "Configure the DJ toggles for the bot",
                subCommands = listOf(
                    SubCommandKt(
                        name = "list",
                        description = "List all DJ toggles."
                    ),
                    SubCommandKt(
                        name = "switch",
                        description = "Switch a specific DJ toggle",
                        options = listOf(
                            CommandOptionKt(
                                name = "toggle",
                                description = "The DJ toggle to switch.",
                                autoComplete = true
                            )
                        )
                    ),
                    SubCommandKt(
                        name = "switchall",
                        description = "Switch all audio commands to be DJ-only."
                    ),
                    SubCommandKt(
                        name = "switchnone",
                        description = "Switch all audio commands to be non-DJ-only."
                    )
                )
            ),
            SubCommandGroupKt(
                name = "logs",
                description = "Configure log toggles for the bot.",
                subCommands = listOf(
                    SubCommandKt(
                        name = "list",
                        description = "List all log toggles."
                    ),
                    SubCommandKt(
                        name = "switch",
                        description = "Switch a specific log toggle.",
                        options = listOf(
                            CommandOptionKt(
                                name = "toggle",
                                description = "The log toggle to switch.",
                                choices = LogTypeKt.toList()
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
        val (_, primaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (primaryCommand) {
            "list" -> event.replyEmbed { handleGeneralToggleList(guild) }.queue()
            "switch" -> event.replyEmbed { handleGeneralSwitch(event) }.queue()
            "dj" -> handleDJ(event)
            "logs" -> event.replyEmbed { handleLog(event) }.queue()
            else -> throw IllegalAccessException("Something went wrong. Primary command: $primaryCommand")
        }
    }

    private fun handleGeneralToggleList(guild: Guild): MessageEmbed {
        val config = TogglesConfigKt(guild)
        val toggleIds = StringBuilder()
        val toggleNames = StringBuilder()
        val toggleStatuses = StringBuilder()
        val embedBuilder = RobertifyEmbedUtilsKt.embedMessage(guild, "\t")

        ToggleKt.values().forEachIndexed { id, toggle ->
            toggleIds.append("${id + 1}\n")
            toggleNames.append("${ToggleKt.parseToggle(toggle)}\n")
            toggleStatuses.append("${if (config.getToggle(toggle)) RobertifyEmojiKt.CHECK_EMOJI.toString() else RobertifyEmojiKt.QUIT_EMOJI.toString()}\n")
        }

        return embedBuilder.addField(
            guild = guild,
            name = TogglesMessages.TOGGLES_EMBED_TOGGLE_ID_FIELD,
            value = toggleIds.toString(),
            inline = true
        ).addField(
            guild = guild,
            name = TogglesMessages.TOGGLES_MESSAGES_EMBED_FEATURE_FIELD,
            value = toggleNames.toString(),
            inline = true
        ).addField(
            guild = guild,
            name = TogglesMessages.TOGGLES_EMBED_STATUS_FIELD,
            value = toggleStatuses.toString(),
            inline = true
        ).build()
    }

    private fun handleGeneralSwitch(event: SlashCommandInteractionEvent): MessageEmbed {
        val guild = event.guild!!
        val toggle = event.getRequiredOption("toggle").asString
        return when (toggle.lowercase()) {
            "restrictedvoice", "1", "rvc", "rvchannels" -> {
                handleSwitch(guild, ToggleKt.RESTRICTED_VOICE_CHANNELS, "Restricted Voice Channels")
            }

            "restrictedtext", "2", "rtc", "rtchannels" -> {
                handleSwitch(guild, ToggleKt.RESTRICTED_TEXT_CHANNELS, "Restricted Text Channels")
            }

            "announcements", "3" -> {
                handleSwitch(guild, ToggleKt.ANNOUNCE_MESSAGES, "Player Announcements")
            }

            "requester", "4" -> {
                handleSwitch(guild, ToggleKt.SHOW_REQUESTER, "Requester Visibility")
            }

            "8ball", "5" -> {
                handleSwitch(guild, ToggleKt.EIGHT_BALL, "8Ball Feature")
            }

            "polls", "6" -> {
                handleSwitch(guild, ToggleKt.POLLS, "Polls Feature")
            }

            "reminders", "7" -> {
                handleSwitch(guild, ToggleKt.REMINDERS, "Reminders Feature")
            }

            "tips", "8" -> {
                handleSwitch(guild, ToggleKt.TIPS, "Tips")
            }

            "voteskips", "voteskip", "vs", "9" -> {
                handleSwitch(
                    guild,
                    ToggleKt.VOTE_SKIPS,
                    "Vote Skips",
                    onEnabled = { _, config ->
                        val skipCommand = SkipCommandKt()
                        if (!config.getDJToggle(skipCommand))
                            config.setDJToggle(skipCommand, true)
                    },
                    onDisabled = { _, config ->
                        val skipCommand = SkipCommandKt()
                        if (config.getDJToggle(skipCommand))
                            event.channel.sendEmbed(guild, TogglesMessages.SKIP_DJ_TOGGLE_PROMPT)
                                .setActionRow(
                                    success(
                                        id = "toggledjskip:yes:${event.user.id}",
                                        emoji = RobertifyEmojiKt.CHECK_EMOJI.emoji
                                    ),
                                    secondary(
                                        id = "toggledjskip:no:${event.user.id}",
                                        emoji = RobertifyEmojiKt.QUIT_EMOJI.emoji
                                    )
                                )
                                .queue()
                    }
                )
            }

            else -> throw IllegalArgumentException("Invalid toggle passed. ($toggle)")
        }
    }

    override suspend fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("toggledjskip:")) return

        val guild = event.guild!!
        val split = event.componentId.split(":")
        if (event.user.id != split[2])
            return event.replyEmbed(GeneralMessages.NO_PERMS_BUTTON)
                .setEphemeral(true)
                .queue()

        val disabledButtons = event.message.buttons.map { it.asDisabled() }

        when (split[1].lowercase()) {
            "yes" -> {
                val skipCommand = SkipCommandKt()
                val config = TogglesConfigKt(guild)
                val localeManager = LocaleManagerKt[guild]

                config.setDJToggle(skipCommand, false)
                event.replyEmbed(
                    TogglesMessages.DJ_TOGGLED,
                    Pair("{command}", "skip"),
                    Pair("{status}", localeManager.getMessage(GeneralMessages.OFF_STATUS))
                )
                    .setEphemeral(true)
                    .queue()
            }

            "no" -> {
                event.replyEmbed(GeneralMessages.OK).setEphemeral(true).queue()
            }
        }

        event.message
            .editMessageComponents(ActionRow.of(disabledButtons))
            .queue()
    }

    private fun handleSwitch(
        guild: Guild,
        toggle: ToggleKt,
        toggleString: String,
        onEnabled: ((guild: Guild, config: TogglesConfigKt) -> Unit)? = null,
        onDisabled: ((guild: Guild, config: TogglesConfigKt) -> Unit)? = null
    ): MessageEmbed {
        val config = TogglesConfigKt(guild)
        val localeManager = LocaleManagerKt[guild]

        return if (config[toggle]) {
            config.setToggle(toggle, false)
            onDisabled?.invoke(guild, config)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TogglesMessages.TOGGLED,
                Pair("{toggle}", "`$toggleString`"),
                Pair("{status}", localeManager.getMessage(GeneralMessages.OFF_STATUS).uppercase())
            ).build()
        } else {
            config.setToggle(toggle, true)
            onEnabled?.invoke(guild, config)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TogglesMessages.TOGGLED,
                Pair("{toggle}", "`$toggleString`"),
                Pair("{status}", localeManager.getMessage(GeneralMessages.ON_STATUS).uppercase())
            ).build()
        }
    }

    private suspend fun handleDJ(event: SlashCommandInteractionEvent) {
        val (_, _, secondaryCommand) = event.fullCommandName.split("\\s".toRegex())
        val guild = event.guild!!

        when (secondaryCommand) {
            "list" -> return displayDJToggles(event)
            "switch" -> event.replyEmbed { handleDJSwitch(guild, event.getRequiredOption("toggle").asString) }
                .queue()

            "switchall" -> {
                val musicCommands = SlashCommandManagerKt.musicCommands
                TogglesConfigKt(guild).setDJToggle(musicCommands, true)
                event.replyEmbed(
                    TogglesMessages.ALL_DJ_TOGGLED,
                    Pair("{status}", LocaleManagerKt[guild].getMessage(GeneralMessages.ON_STATUS).uppercase())
                ).queue()
            }

            "switchnone" -> {
                val musicCommands = SlashCommandManagerKt.musicCommands
                TogglesConfigKt(guild).setDJToggle(musicCommands, false)
                event.replyEmbed(
                    TogglesMessages.ALL_DJ_TOGGLED,
                    Pair("{status}", LocaleManagerKt[guild].getMessage(GeneralMessages.OFF_STATUS).uppercase())
                ).queue()
            }
        }
    }

    private fun handleDJSwitch(guild: Guild, toggle: String): MessageEmbed {
        val command = SlashCommandManagerKt.getCommand(toggle)
            ?: return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TogglesMessages.DJ_TOGGLE_INVALID_COMMAND,
                Pair("{command}", toggle)
            ).build()

        val config = TogglesConfigKt(guild)
        val localeManager = LocaleManagerKt[guild]
        return if (config.getDJToggle(command)) {
            config.setDJToggle(command, false)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TogglesMessages.DJ_TOGGLED,
                Pair("{command}", command.info.name),
                Pair("{status}", localeManager.getMessage(GeneralMessages.OFF_STATUS).uppercase())
            ).build()
        } else {
            config.setDJToggle(command, true)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TogglesMessages.DJ_TOGGLED,
                Pair("{command}", command.info.name),
                Pair("{status}", localeManager.getMessage(GeneralMessages.ON_STATUS).uppercase())
            ).build()
        }
    }

    private suspend fun displayDJToggles(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val musicCommands = SlashCommandManagerKt.musicCommands
        val config = TogglesConfigKt(guild)
        val description = mutableListOf<String>()
        val localeManager = LocaleManagerKt[guild]

        description.add(
            """
            ```md
            # ${localeManager.getMessage(TogglesMessages.TOGGLES_EMBED_STATUS_FIELD)}->${
                localeManager.getMessage(
                    TogglesMessages.DJ_TOGGLES_EMBED_COMMAND_FIELD
                )
            }
            ```
        """.trimIndent()
        )

        musicCommands.sortedWith { first, second ->
            first.info.name.compareTo(second.info.name)
        }.forEach { command ->
            description.add(
                "${
                    if (config.getDJToggle(command))
                        RobertifyEmojiKt.CHECK_EMOJI.toString()
                    else RobertifyEmojiKt.QUIT_EMOJI.toString()
                }\t\t${command.info.name}"
            )
        }

        PaginationHandlerKt.paginateMessage(event, description, 25)
    }

    private fun handleLog(event: SlashCommandInteractionEvent): MessageEmbed {
        val (_, _, secondaryCommand) = event.fullCommandName.split("\\s".toRegex())
        val guild = event.guild!!

        return when (secondaryCommand) {
            "list" -> getLogTogglesEmbed(guild)
            "switch" -> handleLogSwitch(guild, event.getRequiredOption("toggle").asString)
            else -> throw IllegalArgumentException("Invalid secondary option: ($secondaryCommand)")
        }
    }

    private fun getLogTogglesEmbed(guild: Guild): MessageEmbed {
        val config = TogglesConfigKt(guild)
        val logTypes = LogTypeKt.values()
        val toggleNames = StringBuilder()
        val toggleStatuses = StringBuilder()
        val embedBuilder = RobertifyEmbedUtilsKt.embedMessage(guild, "\t")

        logTypes.forEach { type ->
            toggleNames.append("${type.name.lowercase()}\n")
            toggleStatuses.append(
                "${
                    if (config.getLogToggle(type))
                        RobertifyEmojiKt.CHECK_EMOJI.toString()
                    else RobertifyEmojiKt.QUIT_EMOJI.toString()
                }\n"
            )
        }
        embedBuilder.addField(
            guild = guild,
            name = TogglesMessages.LOG_TOGGLES_EMBED_TYPE_FIELD,
            value = toggleNames.toString(),
            inline = true
        )
        embedBuilder.addBlankField(true)
        embedBuilder.addField(
            guild = guild,
            name = TogglesMessages.LOG_TOGGLES_EMBED_STATUS_FIELD,
            value = toggleStatuses.toString(),
            inline = true
        )
        return embedBuilder.build()
    }

    private fun handleLogSwitch(guild: Guild, toggle: String): MessageEmbed {
        val config = TogglesConfigKt(guild)
        val logType = try {
            LogTypeKt.valueOf(toggle)
        } catch (e: IllegalArgumentException) {
            run {
                return RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    TogglesMessages.LOG_TOGGLE_INVALID_TYPE,
                    Pair("{logType}", toggle)
                ).build()
            }
        }

        return if (config.getLogToggle(logType)) {
            config.setLogToggle(logType, false)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TogglesMessages.LOG_TOGGLED,
                Pair("{logType}", logType.getName()),
                Pair("{status}", LocaleManagerKt[guild].getMessage(GeneralMessages.OFF_STATUS).uppercase())
            ).build()
        } else {
            config.setLogToggle(logType, true)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TogglesMessages.LOG_TOGGLED,
                Pair("{logType}", logType.getName()),
                Pair("{status}", LocaleManagerKt[guild].getMessage(GeneralMessages.ON_STATUS).uppercase())
            ).build()
        }
    }

    override suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "toggles" && event.subcommandGroup != "dj" && event.focusedOption.name != "toggle")
            return

        val focusedValue = event.focusedOption.value
        if (focusedValue.isEmpty())
            return event.replyChoices().queue()

        val musicCommands = SlashCommandManagerKt.musicCommands
            .filter { it.info.name.lowercase().contains(event.focusedOption.value.lowercase()) }
            .map { it.info.name }
            .coerceAtMost(InteractionLimitsKt.AUTO_COMPLETE_CHOICES)

        event.replyChoiceStrings(musicCommands).queue()
    }
}