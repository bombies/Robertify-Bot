package main.commands.slashcommands.util

import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.database.mongodb.cache.BotDBCache
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class UptimeCommand : AbstractSlashCommand(
    SlashCommand(
        name = "uptime",
        description = "Get how long the bot has been online.",
        guildUseOnly = false
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(
            GeneralUtils.getDurationString(System.currentTimeMillis() - BotDBCache.instance.lastStartup)
        )
            .queue()
    }

    override val help: String
        get() = "Get how long the bot has been online."
}