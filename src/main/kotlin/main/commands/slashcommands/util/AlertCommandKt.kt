package main.commands.slashcommands.util

import main.constants.TimeFormatKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.AlertMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class AlertCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "alert",
        description = "View the latest alert from the developer.",
        guildUseOnly = false
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val botDb = BotDBCacheKt.instance
        val guild = event.guild
        val user = event.user
        val latestAlert = botDb.latestAlert
        val localeManager = LocaleManagerKt[guild]

        if (latestAlert.first.isNotEmpty() && latestAlert.first.isNotBlank())
            botDb.addAlertViewer(user.idLong)

        event.replyEmbeds(
            RobertifyEmbedUtilsKt.embedMessageWithTitle(
                guild, AlertMessages.ALERT_EMBED_TITLE,
                if (latestAlert.first.isEmpty() || latestAlert.first.isBlank())
                    localeManager.getMessage(AlertMessages.NO_ALERT)
                else latestAlert.first
            )
                .setFooter(
                    localeManager.getMessage(
                        AlertMessages.ALERT_EMBED_FOOTER,
                        Pair("{number}", botDb.getPosOfAlertViewer(user.idLong).toString()),
                        Pair("{alertDate}", GeneralUtilsKt.formatDate(latestAlert.second, TimeFormatKt.DD_MMMM_YYYY))
                    )
                )
                .build()
        ).queue()
    }

}