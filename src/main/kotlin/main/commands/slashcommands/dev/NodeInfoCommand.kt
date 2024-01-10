package main.commands.slashcommands.dev

import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.math.roundToInt

class NodeInfoCommand : AbstractSlashCommand(
    SlashCommand(
        name = "nodeinfo",
        description = "Count all the voice channels Robertify is currenly playing music in",
        developerOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val lavalink = Robertify.lavalink
        val desc = "```txt\n${
            lavalink.nodes.joinToString("\n") { node ->
                val stats = node.stats

                if (stats == null)
                    """
                     ===============================
                     ✨ ${node.name.replace("_", " ")} ✨
                     
                     There is no information yet!
                     
                     ===============================
                    """.trimIndent()
                else

                    """
                     ===============================
                     ✨ ${node.name.replace("_", " ")} ✨
                     
                     CPU Cores: ${stats.cpu.cores}
                     Total LavaLink Load: ${(stats.cpu.lavalinkLoad * 100).roundToInt()}%
                     Total System Load: ${(stats.cpu.systemLoad * 100).roundToInt()}%
                     -------------------------------
                     Memory Allocated: ${(stats.memory.allocated / 1000000)}MB
                     Memory Reservable: ${(stats.memory.reservable / 1000000)}MB
                     Memory Used: ${(stats.memory.used / 1000000)}MB
                     Memory Free: ${(stats.memory.free / 1000000)}MB
                     -------------------------------
                     Total Players: ${stats.players}
                     Playing Players: ${stats.playingPlayers}
                     Frames Sent/Minute: ${stats.frameStats?.sent ?: "Unknown"} 
                     Frames Nulled/Minute: ${stats.frameStats?.nulled ?: "Unknown"} 
                     Frames Deficit/Minute: ${stats.frameStats?.deficit ?: "Unknown"} 
                     -------------------------------
                     Uptime: ${GeneralUtils.getDurationString(stats.uptime)}
                     ===============================
                """.trimIndent()
            }
        }```"
        event.replyEmbed(desc).setEphemeral(true).queue()
    }

}