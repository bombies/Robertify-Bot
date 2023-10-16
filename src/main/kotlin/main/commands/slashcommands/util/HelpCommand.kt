package main.commands.slashcommands.util

import main.commands.slashcommands.SlashCommandManager
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.RobertifyTheme
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.themes.ThemesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.HelpMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu

class HelpCommand : AbstractSlashCommand({
    name = "help"
    description = "See all the commands the bot has to offer to you."
    options = listOf(
        CommandOption(
            name = "command",
            description = "View help for a specific command.",
            required = false
        )
    )
    guildUseOnly = false
}) {

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild
        val localeManager = LocaleManager[guild]

        if (event.options.isEmpty()) {
            val embed = RobertifyEmbedUtils.embedMessage(guild, HelpMessages.HELP_EMBED_DESC)
                .addField(
                    "💼 ${localeManager.getMessage(HelpMessages.HELP_MANAGEMENT_OPTION)}",
                    localeManager.getMessage(HelpMessages.HELP_MANAGEMENT_OPTION_DESC),
                    true
                )
                .addField(
                    "🎶 ${localeManager.getMessage(HelpMessages.HELP_MUSIC_OPTION)}",
                    localeManager.getMessage(HelpMessages.HELP_MUSIC_OPTION_DESC),
                    true
                )
                .addBlankField(true)
                .addField(
                    "⚒️ ${localeManager.getMessage(HelpMessages.HELP_MISCELLANEOUS_OPTION)}",
                    localeManager.getMessage(HelpMessages.HELP_MISCELLANEOUS_OPTION_DESC),
                    true
                )
                .addField(
                    "❓ ${localeManager.getMessage(HelpMessages.HELP_UTILITY_OPTION)}",
                    localeManager.getMessage(HelpMessages.HELP_UTILITY_OPTION_DESC),
                    true
                )
                .addBlankField(true)
                .build()
            event.replyEmbeds(embed)
                .addActionRow(getSelectionMenu(guild, event.user.idLong))
                .setEphemeral(true)
                .queue()
        } else {
            val command = event.getRequiredOption("command").asString
            event.replyEmbed { searchCommand(command, guild) }
                .setEphemeral(true)
                .queue()
        }

    }

    override fun onStringSelect(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith("menu:help")) return
        val guild = event.guild

        if (event.componentId.split(":")[2] != event.user.id)
            return event.replyEmbed(GeneralMessages.NO_MENU_PERMS).queue()

        when (event.selectedOptions.first().value) {
            "help:management" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MANAGEMENT)).queue()
            "help:music" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MUSIC)).queue()
            "help:misc" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MISCELLANEOUS)).queue()
            "help:utility" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.UTILITY)).queue()
        }
    }

    private fun getSelectionMenu(guild: Guild?, userId: Long): SelectMenu {
        val localeManager = LocaleManager[guild]
        return StringSelectionMenuBuilder(
            _name = "menu:help",
            placeholder = localeManager.getMessage(GeneralMessages.SELECT_MENU_PLACEHOLDER),
            range = Pair(1, 1),
            limitedTo = userId,
            _options = listOf(
                StringSelectMenuOption(
                    label = localeManager.getMessage(HelpMessages.HELP_MANAGEMENT_OPTION),
                    value = "help:management",
                    emoji = Emoji.fromUnicode("💼")
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(HelpMessages.HELP_MUSIC_OPTION),
                    value = "help:music",
                    emoji = Emoji.fromUnicode("🎶")
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(HelpMessages.HELP_MISCELLANEOUS_OPTION),
                    value = "help:misc",
                    emoji = Emoji.fromUnicode("⚒️")
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(HelpMessages.HELP_UTILITY_OPTION),
                    value = "help:utility",
                    emoji = Emoji.fromUnicode("❓")
                ),
            )
        ).build()
    }

    private fun searchCommand(search: String, guild: Guild?): MessageEmbed {
        val command = SlashCommandManager.getCommand(search)
            ?: return RobertifyEmbedUtils.embedMessage(
                guild,
                HelpMessages.HELP_NOTHING_FOUND,
                Pair("{command}", search)
            )
                .build()

        val theme = if (guild != null) ThemesConfig(guild).getTheme() else RobertifyTheme.GREEN
        val localeManager = LocaleManager[guild]

        return RobertifyEmbedUtils.embedMessage(guild, command.help)
            .setAuthor(
                "${localeManager.getMessage(HelpMessages.HELP_EMBED_AUTHOR)} [${command.info.name}]",
                null,
                theme.transparent
            ).build()
    }

    private fun getHelpEmbed(guild: Guild?, type: HelpType): MessageEmbed {
        val commandManager = SlashCommandManager
        val localeManager = LocaleManager[guild]
        val embedBuilder = RobertifyEmbedUtils.embedMessage(guild, HelpMessages.HELP_COMMANDS, Pair("{prefix}", "/"))
        val helpTypeFieldName: String

        val commands = when (type) {
            HelpType.MANAGEMENT -> {
                helpTypeFieldName = localeManager.getMessage(HelpMessages.HELP_MANAGEMENT_OPTION)
                commandManager.managementCommands
            }

            HelpType.MUSIC -> {
                helpTypeFieldName = localeManager.getMessage(HelpMessages.HELP_MUSIC_OPTION)
                commandManager.musicCommands
            }

            HelpType.MISCELLANEOUS -> {
                helpTypeFieldName = localeManager.getMessage(HelpMessages.HELP_MISCELLANEOUS_OPTION)
                commandManager.miscCommands
            }

            HelpType.UTILITY -> {
                helpTypeFieldName = localeManager.getMessage(HelpMessages.HELP_UTILITY_OPTION)
                commandManager.utilityCommands
            }
        }

        return embedBuilder.addField(
            helpTypeFieldName,
            commands.joinToString(", ") { "`${it.info.name}`" },
            false
        ).build()
    }

    private enum class HelpType {
        MANAGEMENT,
        MUSIC,
        MISCELLANEOUS,
        UTILITY
    }
}