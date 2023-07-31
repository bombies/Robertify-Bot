package main.commands.slashcommands.util

import main.constants.TimeFormat
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.locale.LocaleManager
import main.utils.locale.messages.AlertMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class AlertCommand : AbstractSlashCommand(
    SlashCommand(
        name = "alert",
        description = "View the latest alert from the developer.",
        guildUseOnly = false
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val botDb = BotDBCache.instance
        val guild = event.guild
        val user = event.user
        val latestAlert = botDb.latestAlert
        val localeManager = LocaleManager[guild]

        if (latestAlert.first.isNotEmpty() && latestAlert.first.isNotBlank())
            botDb.addAlertViewer(user.idLong)

        event.replyEmbeds(
            RobertifyEmbedUtils.embedMessageWithTitle(
                guild, AlertMessages.ALERT_EMBED_TITLE,
                if (latestAlert.first.isEmpty() || latestAlert.first.isBlank())
                    localeManager.getMessage(AlertMessages.NO_ALERT)
                else latestAlert.first
            )
                .setFooter(
                    localeManager.getMessage(
                        AlertMessages.ALERT_EMBED_FOOTER,
                        Pair("{number}", botDb.getPosOfAlertViewer(user.idLong).toString()),
                        Pair("{alertDate}", GeneralUtils.formatDate(latestAlert.second, TimeFormat.DD_MMMM_YYYY))
                    )
                )
                .build()
        ).queue()
    }

}