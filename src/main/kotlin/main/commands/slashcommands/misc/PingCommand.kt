package main.commands.slashcommands.misc

import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.math.roundToInt

class PingCommand : AbstractSlashCommand(Command(
    name = "ping",
    description = "Check the ping of the bot to Discord's servers.",
    guildUseOnly = false
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.jda.restPing.queue { ping ->
            event.replyEmbed("""
                🏓 **Pong!**
                
                REST Ping: **${ping}ms**
                Websocket Ping: **${event.jda.shardManager!!.averageGatewayPing.roundToInt()}**
            """.trimIndent())
                .queue()
        }
    }

    override val help: String
        get() = "Shows the bot's ping to Discord's servers."
}