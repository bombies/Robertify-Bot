package main.commands.slashcommands.util

import dev.minn.jda.ktx.interactions.components.link
import main.constants.RobertifyThemeKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.BotInfoMessages
import main.utils.RobertifyEmbedUtilsKt.Companion.addField
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Instant

class BotInfoCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "botinfo",
        description = "View some cool information about Robertify.",
        guildUseOnly = false
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild
        val localeManager = LocaleManagerKt[guild]
        val botDb = BotDBCacheKt.instance

        event.replyEmbeds(
            RobertifyEmbedUtilsKt.embedMessage(guild, "\t")
                .setThumbnail(if (guild != null) ThemesConfigKt(guild).theme.transparent else RobertifyThemeKt.GREEN.transparent)
                .addField(
                    guild = guild,
                    name = BotInfoMessages.BOT_INFO_DEVELOPERS,
                    value = botDb.getDevelopers().joinToString(", ") { "<@$it>" }
                )
                .addField(
                    guild = guild,
                    name = BotInfoMessages.BOT_INFO_ABOUT_ME_LABEL,
                    value = BotInfoMessages.BOT_INFO_ABOUT_ME_VALUE
                )
                .addField(
                    guild = guild,
                    name = BotInfoMessages.BOT_INFO_UPTIME,
                    value = GeneralUtilsKt.getDurationString(System.currentTimeMillis() - botDb.lastStartup)
                )
                .setTimestamp(Instant.now())
                .build()
        ).addActionRow(
            link(
                url = "https://robertify.me/terms",
                label = localeManager.getMessage(BotInfoMessages.BOT_INFO_TERMS),
                emoji = RobertifyThemeKt.ORANGE.emoji
            ),
            link(
                url = "https://robertify.me/privacypolicy",
                label = localeManager.getMessage(BotInfoMessages.BOT_INFO_PRIVACY),
                emoji = RobertifyThemeKt.BLUE.emoji
            )
        ).queue()
    }
}