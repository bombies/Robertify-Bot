package main.commands.slashcommands.management

import main.main.Robertify
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.LocaleManager
import main.utils.locale.RobertifyLocale
import main.utils.locale.messages.LanguageCommandMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.sharding.ShardManager

class LanguageCommand : AbstractSlashCommand(
    SlashCommand(
        name = "language",
        description = "Change the bot's language.",
        options = listOf(
            CommandOption(
                name = "language",
                description = "The specific language to switch to.",
                choices = RobertifyLocale.availableLanguages.map { it.name },
                required = false
            )
        ),
        adminOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val localeManager = LocaleManager[guild]
        val language = event.getOption("language")?.asString

        if (language == null) {
            event.replyEmbed {
                embed(
                    LanguageCommandMessages.LANGUAGE_EMBED_DESC,
                    Pair("{language}", "${localeManager.locale.name} ${localeManager.locale.flag}")
                )
            }.setActionRow(StringSelectionMenuBuilder(
                _name = "languagemenu",
                placeholder = localeManager.getMessage(LanguageCommandMessages.LANGUAGE_SELECT_MENU_PLACE_HOLDER),
                range = Pair(1, 1),
                _options = RobertifyLocale.availableLanguages.map { locale ->
                    StringSelectMenuOption(
                        label = locale.localName,
                        value = "languagemenu:${locale.name.lowercase()}",
                        emoji = Emoji.fromUnicode(locale.flag)
                    )
                }
            ).build())
                .setEphemeral(true)
                .queue()
        } else {
            val newLocale = RobertifyLocale.parse(language)
            localeManager.locale = newLocale
            RequestChannelConfig(guild).updateAll()
            event.replyEmbed {
                embed(
                    LanguageCommandMessages.LANGUAGE_CHANGED,
                    Pair("{language}", newLocale.localName)
                )
            }.setEphemeral(true)
                .queue()
        }
    }

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        val selectMenu = event.selectMenu
        if (selectMenu.id?.startsWith("languagemenu") != true)
            return

        val guild = event.guild!!

        event.deferReply(true).queue()

        val (_, locale) = event.selectedOptions.first().value.split(":")
        val newLocale = setLocale(guild, locale)

        event.hook.sendEmbed(guild) {
            embed(
                LanguageCommandMessages.LANGUAGE_CHANGED,
                Pair("{language}", newLocale.localName)
            )
        }.queue()
    }

    suspend fun setLocale(guild: Guild, locale: String, shardManager: ShardManager = Robertify.shardManager): RobertifyLocale {
        val newLocale = RobertifyLocale.parse(locale)
        LocaleManager[guild].locale = newLocale
        RequestChannelConfig(guild, shardManager).updateAll()
        return newLocale
    }
}