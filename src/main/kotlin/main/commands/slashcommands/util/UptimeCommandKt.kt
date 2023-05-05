package main.commands.slashcommands.util

import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class UptimeCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "uptime",
        description = "Get how long the bot has been online.",
        guildUseOnly = false
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(
            event.guild,
            GeneralUtilsKt.getDurationString(System.currentTimeMillis() - BotDBCacheKt.instance.lastStartup)
        )
            .queue()
    }

    override val help: String
        get() = "Get how long the bot has been online."
}