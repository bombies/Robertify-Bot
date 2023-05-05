package main.commands.slashcommands.util

import main.commands.slashcommands.SlashCommandManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.RobertifyThemeKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilderKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.HelpMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu

class HelpCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "help",
        description = "See all the commands the bot has to offer to you.",
        options = listOf(
            CommandOptionKt(
                name = "command",
                description = "View help for a specific command.",
                required = false
            )
        ),
        guildUseOnly = false
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild
        val localeManager = LocaleManagerKt[guild]

        if (event.options.isEmpty()) {
            val embed = RobertifyEmbedUtilsKt.embedMessage(guild, HelpMessages.HELP_EMBED_DESC)
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

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith("menu:help")) return
        val guild = event.guild

        if (event.componentId.split(":")[2] != event.user.id)
            return event.replyEmbed(guild, GeneralMessages.NO_MENU_PERMS).queue()

        when (event.selectedOptions.first().value) {
            "help:management" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MANAGEMENT)).queue()
            "help:music" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MUSIC)).queue()
            "help:misc" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.MISCELLANEOUS)).queue()
            "help:utility" -> event.editMessageEmbeds(getHelpEmbed(guild, HelpType.UTILITY)).queue()
        }
    }

    private fun getSelectionMenu(guild: Guild?, userId: Long): SelectMenu {
        val localeManager = LocaleManagerKt[guild]
        return StringSelectionMenuBuilderKt(
            _name = "menu:help",
            placeholder = localeManager.getMessage(GeneralMessages.SELECT_MENU_PLACEHOLDER),
            range = Pair(1, 1),
            limitedTo = userId,
            _options = listOf(
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(HelpMessages.HELP_MANAGEMENT_OPTION),
                    value = "help:management",
                    emoji = Emoji.fromUnicode("💼")
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(HelpMessages.HELP_MUSIC_OPTION),
                    value = "help:music",
                    emoji = Emoji.fromUnicode("🎶")
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(HelpMessages.HELP_MISCELLANEOUS_OPTION),
                    value = "help:misc",
                    emoji = Emoji.fromUnicode("⚒️")
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(HelpMessages.HELP_UTILITY_OPTION),
                    value = "help:utility",
                    emoji = Emoji.fromUnicode("❓")
                ),
            )
        ).build()
    }

    private fun searchCommand(search: String, guild: Guild?): MessageEmbed {
        val command = SlashCommandManagerKt.getCommand(search)
            ?: return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                HelpMessages.HELP_NOTHING_FOUND,
                Pair("{command}", search)
            )
                .build()

        val theme = if (guild != null) ThemesConfigKt(guild).theme else RobertifyThemeKt.GREEN
        val localeManager = LocaleManagerKt[guild]

        return RobertifyEmbedUtilsKt.embedMessage(guild, command.help)
            .setAuthor(
                "${localeManager.getMessage(HelpMessages.HELP_EMBED_AUTHOR)} [${command.info.name}]",
                null,
                theme.transparent
            ).build()
    }

    private fun getHelpEmbed(guild: Guild?, type: HelpType): MessageEmbed {
        val commandManager = SlashCommandManagerKt
        val localeManager = LocaleManagerKt[guild]
        val embedBuilder = RobertifyEmbedUtilsKt.embedMessage(guild, HelpMessages.HELP_COMMANDS, Pair("{prefix}", "/"))
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