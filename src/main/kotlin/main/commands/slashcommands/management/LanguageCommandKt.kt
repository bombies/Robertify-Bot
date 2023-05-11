package main.commands.slashcommands.management

import main.main.RobertifyKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilderKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.RobertifyLocaleKt
import main.utils.locale.messages.LanguageCommandMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.sharding.ShardManager

class LanguageCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "language",
        description = "Change the bot's language.",
        options = listOf(
            CommandOptionKt(
                name = "language",
                description = "The specific language to switch to.",
                choices = RobertifyLocaleKt.availableLanguages.map { it.name },
                required = false
            )
        ),
        adminOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val localeManager = LocaleManagerKt[guild]
        val language = event.getOption("language")?.asString

        if (language == null) {
            event.replyEmbed {
                embed(
                    LanguageCommandMessages.LANGUAGE_EMBED_DESC,
                    Pair("{language}", "${localeManager.locale.name} ${localeManager.locale.flag}")
                )
            }.setActionRow(StringSelectionMenuBuilderKt(
                _name = "languagemenu",
                placeholder = localeManager.getMessage(LanguageCommandMessages.LANGUAGE_SELECT_MENU_PLACE_HOLDER),
                range = Pair(1, 1),
                _options = RobertifyLocaleKt.availableLanguages.map { locale ->
                    StringSelectMenuOptionKt(
                        label = locale.localName,
                        value = "languagemenu:${locale.name.lowercase()}",
                        emoji = Emoji.fromUnicode(locale.flag)
                    )
                }
            ).build())
                .setEphemeral(true)
                .queue()
        } else {
            val newLocale = RobertifyLocaleKt.parse(language)
            localeManager.locale = newLocale
            RequestChannelConfigKt(guild).updateAll()
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

    fun setLocale(guild: Guild, locale: String, shardManager: ShardManager = RobertifyKt.shardManager): RobertifyLocaleKt {
        val newLocale = RobertifyLocaleKt.parse(locale)
        LocaleManagerKt[guild].locale = newLocale
        RequestChannelConfigKt(guild, shardManager).updateAll()
        return newLocale
    }
}