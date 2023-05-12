package main.commands.slashcommands.util

import dev.minn.jda.ktx.interactions.components.link
import main.constants.RobertifyTheme
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.json.themes.ThemesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.BotInfoMessages
import main.utils.RobertifyEmbedUtils.Companion.addField
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Instant

class BotInfoCommand : AbstractSlashCommand(
    Command(
        name = "botinfo",
        description = "View some cool information about Robertify.",
        guildUseOnly = false
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild
        val localeManager = LocaleManager[guild]
        val botDb = BotDBCache.instance

        event.replyEmbeds(
            RobertifyEmbedUtils.embedMessage(guild, "\t")
                .setThumbnail(if (guild != null) ThemesConfig(guild).theme.transparent else RobertifyTheme.GREEN.transparent)
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
                    value = GeneralUtils.getDurationString(System.currentTimeMillis() - botDb.lastStartup)
                )
                .setTimestamp(Instant.now())
                .build()
        ).addActionRow(
            link(
                url = "https://robertify.me/terms",
                label = localeManager.getMessage(BotInfoMessages.BOT_INFO_TERMS),
                emoji = RobertifyTheme.ORANGE.emoji
            ),
            link(
                url = "https://robertify.me/privacypolicy",
                label = localeManager.getMessage(BotInfoMessages.BOT_INFO_PRIVACY),
                emoji = RobertifyTheme.BLUE.emoji
            )
        ).queue()
    }
}