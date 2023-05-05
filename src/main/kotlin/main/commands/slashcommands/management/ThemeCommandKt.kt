package main.commands.slashcommands.management

import main.constants.RobertifyPermissionKt
import main.constants.RobertifyThemeKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.hasPermissions
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilderKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.ThemeMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class ThemeCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "themes",
        description = "Set the theme of the bot.",
        _requiredPermissions = listOf(RobertifyPermissionKt.ROBERTIFY_THEME),
        isPremium = true,
        options = listOf(
            CommandOptionKt(
                name = "theme",
                description = "The theme to set as the bot's current theme.",
                choices = RobertifyThemeKt.values().map { it.name.uppercase() },
                required = false
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val choice = event.getOption("theme")
        val guild = event.guild!!

        if (choice == null) {
            event.replyWithEmbed(guild) {
                embed(
                    title = ThemeMessages.THEME_EMBED_TITLE,
                    description = ThemeMessages.THEME_EMBED_DESC
                )
            }.addActionRow(getSelectMenu(guild, event.user.idLong))
                .queue()
        } else {
            val theme = RobertifyThemeKt.parse(choice.asString)
            updateTheme(guild, theme)
            event.replyWithEmbed(
                guild,
                ThemeMessages.THEME_SET,
                Pair("{theme}", theme.name.replace("_", " "))
            ).setEphemeral(true)
                .queue()
        }
    }

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith("menu:themes")) return
        val guild = event.guild!!

        if (!event.member!!.hasPermissions(RobertifyPermissionKt.ROBERTIFY_THEME))
            return event.replyWithEmbed(
                guild,
                GeneralMessages.INSUFFICIENT_PERMS,
                Pair("{permissions}", RobertifyPermissionKt.ROBERTIFY_THEME.name)
            ).setEphemeral(true)
                .queue()

        if (event.componentId.split(":")[2] != event.user.id)
            return event.replyWithEmbed(guild, GeneralMessages.NO_MENU_PERMS)
                .setEphemeral(true)
                .queue()

        val optionSelected = event.selectedOptions[0]
        val theme = RobertifyThemeKt.parse(optionSelected.value.split(":")[1].lowercase())
        updateTheme(guild, theme)

        event.replyWithEmbed(guild) {
            embedBuilder(ThemeMessages.THEME_SET, Pair("{theme}", theme.name.replace("_", " ")))
                .setImage(theme.transparent)
                .build()
        }.setEphemeral(true)
            .queue()
    }

    private fun updateTheme(guild: Guild, theme: RobertifyThemeKt) {
        ThemesConfigKt(guild).theme = theme
        GeneralUtilsKt.setDefaultEmbed(guild)
        RequestChannelConfigKt(guild).updateMessage()
    }

    private fun getSelectMenu(guild: Guild, userId: Long): StringSelectMenu {
        val localeManager = LocaleManagerKt[guild]
        return StringSelectionMenuBuilderKt(
            _name = "menu:themes",
            placeholder = localeManager.getMessage(ThemeMessages.THEME_SELECT_MENU_PLACEHOLDER),
            range = Pair(1, 1),
            limitedTo = userId,
            _options = listOf(
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_GREEN),
                    value = "themes:green",
                    emoji = RobertifyThemeKt.GREEN.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_MINT),
                    value = "themes:mint",
                    emoji = RobertifyThemeKt.MINT.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_GOLD),
                    value = "themes:gold",
                    emoji = RobertifyThemeKt.GOLD.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_RED),
                    value = "themes:red",
                    emoji = RobertifyThemeKt.RED.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_PASTEL_RED),
                    value = "themes:pastel_red",
                    emoji = RobertifyThemeKt.PASTEL_RED.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_PINK),
                    value = "themes:pink",
                    emoji = RobertifyThemeKt.PINK.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_PURPLE),
                    value = "themes:purple",
                    emoji = RobertifyThemeKt.PURPLE.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_PASTEL_PURPLE),
                    value = "themes:pastel_purple",
                    emoji = RobertifyThemeKt.PASTEL_PURPLE.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_BLUE),
                    value = "themes:blue",
                    emoji = RobertifyThemeKt.BLUE.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_LIGHT_BLUE),
                    value = "themes:lightblue",
                    emoji = RobertifyThemeKt.LIGHT_BLUE.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_BABY_BLUE),
                    value = "themes:baby_blue",
                    emoji = RobertifyThemeKt.BABY_BLUE.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_ORANGE),
                    value = "themes:orange",
                    emoji = RobertifyThemeKt.ORANGE.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_YELLOW),
                    value = "themes:yellow",
                    emoji = RobertifyThemeKt.YELLOW.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_PASTEL_YELLOW),
                    value = "themes:pastel_yellow",
                    emoji = RobertifyThemeKt.PASTEL_YELLOW.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_DARK),
                    value = "themes:dark",
                    emoji = RobertifyThemeKt.DARK.emoji
                ),
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(ThemeMessages.THEME_LIGHT),
                    value = "themes:light",
                    emoji = RobertifyThemeKt.LIGHT.emoji
                )
            )
        ).build()
    }

    override val help: String
        get() = "Tired of seeing our boring old green theme? Well, using this command you can have " +
                "**10** other colours to choose from! It's as easy as selecting the colour you want from the selection menu provided."
}