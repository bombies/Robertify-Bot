package main.commands.slashcommands.management

import main.constants.RobertifyPermission
import main.constants.RobertifyTheme
import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.GeneralUtils.hasPermissions
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.json.themes.ThemesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.ThemeMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.sharding.ShardManager

class ThemeCommand : AbstractSlashCommand(
    SlashCommand(
        name = "themes",
        description = "Set the theme of the bot.",
        _requiredPermissions = listOf(RobertifyPermission.ROBERTIFY_THEME),
        isPremium = true,
        options = listOf(
            CommandOption(
                name = "theme",
                description = "The theme to set as the bot's current theme.",
                choices = RobertifyTheme.entries.map { it.name.uppercase() },
                required = false
            )
        )
    )
) {

    override fun handle(event: SlashCommandInteractionEvent) {
        val choice = event.getOption("theme")
        val guild = event.guild!!

        event.deferReply(true).queue()

        if (choice == null) {
            event.hook.sendEmbed {
                embed(
                    title = ThemeMessages.THEME_EMBED_TITLE,
                    description = ThemeMessages.THEME_EMBED_DESC
                )
            }.addActionRow(getSelectMenu(guild, event.user.idLong))
                .queue()
        } else {
            val theme = RobertifyTheme.parse(choice.asString)
            updateTheme(guild, theme)
            event.hook.sendEmbed(guild) {
                embed(
                    ThemeMessages.THEME_SET,
                    Pair("{theme}", theme.name.replace("_", " "))
                )
            }.queue()
        }
    }

    override fun onStringSelect(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith("menu:themes")) return

        event.deferReply(true).queue()

        val guild = event.guild!!

        if (!event.member!!.hasPermissions(RobertifyPermission.ROBERTIFY_THEME))
            return event.replyEmbed(
                GeneralMessages.INSUFFICIENT_PERMS,
                Pair("{permissions}", RobertifyPermission.ROBERTIFY_THEME.name)
            ).setEphemeral(true)
                .queue()

        if (event.componentId.split(":")[2] != event.user.id)
            return event.replyEmbed(GeneralMessages.NO_MENU_PERMS)
                .setEphemeral(true)
                .queue()

        val optionSelected = event.selectedOptions[0]
        val theme = RobertifyTheme.parse(optionSelected.value.split(":")[1].lowercase())
        updateTheme(guild, theme)

        event.hook.sendEmbed {
            embedBuilder(ThemeMessages.THEME_SET, Pair("{theme}", theme.name.replace("_", " ")))
                .setImage(theme.transparent)
                .build()
        }.queue()
    }

    fun updateTheme(guild: Guild, theme: RobertifyTheme, shardManager: ShardManager = Robertify.shardManager) {
        ThemesConfig(guild).setTheme(theme)
        GeneralUtils.setDefaultEmbed(guild)
        RequestChannelConfig(guild, shardManager).updateMessage()
    }

    private fun getSelectMenu(guild: Guild, userId: Long): StringSelectMenu {
        val localeManager = LocaleManager[guild]
        return StringSelectionMenuBuilder(
            _name = "menu:themes",
            placeholder = localeManager.getMessage(ThemeMessages.THEME_SELECT_MENU_PLACEHOLDER),
            range = Pair(1, 1),
            limitedTo = userId,
            _options = listOf(
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_GREEN),
                    value = "themes:green",
                    emoji = RobertifyTheme.GREEN.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_MINT),
                    value = "themes:mint",
                    emoji = RobertifyTheme.MINT.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_GOLD),
                    value = "themes:gold",
                    emoji = RobertifyTheme.GOLD.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_RED),
                    value = "themes:red",
                    emoji = RobertifyTheme.RED.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_PASTEL_RED),
                    value = "themes:pastel_red",
                    emoji = RobertifyTheme.PASTEL_RED.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_PINK),
                    value = "themes:pink",
                    emoji = RobertifyTheme.PINK.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_PURPLE),
                    value = "themes:purple",
                    emoji = RobertifyTheme.PURPLE.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_PASTEL_PURPLE),
                    value = "themes:pastel_purple",
                    emoji = RobertifyTheme.PASTEL_PURPLE.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_BLUE),
                    value = "themes:blue",
                    emoji = RobertifyTheme.BLUE.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_LIGHT_BLUE),
                    value = "themes:lightblue",
                    emoji = RobertifyTheme.LIGHT_BLUE.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_BABY_BLUE),
                    value = "themes:baby_blue",
                    emoji = RobertifyTheme.BABY_BLUE.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_ORANGE),
                    value = "themes:orange",
                    emoji = RobertifyTheme.ORANGE.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_YELLOW),
                    value = "themes:yellow",
                    emoji = RobertifyTheme.YELLOW.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_PASTEL_YELLOW),
                    value = "themes:pastel_yellow",
                    emoji = RobertifyTheme.PASTEL_YELLOW.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_DARK),
                    value = "themes:dark",
                    emoji = RobertifyTheme.DARK.emoji
                ),
                StringSelectMenuOption(
                    label = localeManager.getMessage(ThemeMessages.THEME_LIGHT),
                    value = "themes:light",
                    emoji = RobertifyTheme.LIGHT.emoji
                )
            )
        ).build()
    }

    override val help: String
        get() = "Tired of seeing our boring old green theme? Well, using this command you can have " +
                "**10** other colours to choose from! It's as easy as selecting the colour you want from the selection menu provided."
}