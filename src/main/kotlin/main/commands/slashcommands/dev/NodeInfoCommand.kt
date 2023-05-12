package main.commands.slashcommands.dev

import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.math.roundToInt

class NodeInfoCommand : AbstractSlashCommand(SlashCommand(
    name = "nodeinfo",
    description = "Count all the voice channels Robertify is currenly playing music in",
    developerOnly = true
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val lavalink = Robertify.lavalink
        val desc = "```txt\n${
            lavalink.nodes.joinToString("\n") { node ->
                val stats = node.stats!!
                """
                     ===============================
                     ✨ ${node.name.replace("_", " ")} ✨
                     
                     CPU Cores: ${stats.cpuCores}
                     Total Lavalink Load: ${(stats.lavalinkLoad * 100).roundToInt()}%
                     Total Sytem Load: ${(stats.systemLoad * 100).roundToInt()}%
                     -------------------------------
                     Memory Allocated: ${(stats.memAllocated / 1000000)}MB
                     Memory Reservable: ${(stats.memReservable / 1000000)}MB
                     Memory Used: ${(stats.memUsed / 1000000)}MB
                     Memory Free: ${(stats.memFree / 1000000)}MB
                     -------------------------------
                     Total Players: ${stats.players}
                     Playing Players: ${stats.playingPlayers}
                     Frames Sent/Minute: ${stats.avgFramesSentPerMinute} 
                     Frames Nulled/Minute: ${stats.avgFramesNulledPerMinute} 
                     Frames Deficit/Minute: ${stats.avgFramesDeficitPerMinute} 
                     -------------------------------
                     Uptime: ${GeneralUtils.getDurationString(stats.uptime)}
                     ===============================
                """.trimIndent()
            }
        }```"
        event.replyEmbed(desc).setEphemeral(true).queue()
    }

}