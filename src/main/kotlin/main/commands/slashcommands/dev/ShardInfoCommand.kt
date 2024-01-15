package main.commands.slashcommands.dev

import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.math.roundToInt

class ShardInfoCommand : AbstractSlashCommand(
    SlashCommand(
        name = "shardinfo",
        description = "Get the information about all shards related to the bot.",
        developerOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val embedBuilder = RobertifyEmbedUtils.getEmbedBuilder(guild)
        val currentShard = event.jda.shardInfo.shardId
        val shardManager = event.jda.shardManager!!
        val shardCache = shardManager.shardCache
        val shards = shardCache.asList().reversed()
        val restPings = mutableListOf<Long>()

        shards.forEach { shard ->
            val restPing = shard.restPing.complete()
            restPings.add(restPing)

            val description = """
                - Status: `${shard.shardStatus}`
                - Gateway Ping: `${shard.gatewayPing}`
                - REST Ping: `$restPing`
                - Guilds: `${shard.guildCache.size()}`
            """.trimIndent()

            val shardId = shard.shardInfo.shardId
            embedBuilder.addField(
                "Shard $shardId ${
                    if (shardId == currentShard)
                        "(current)"
                    else ""
                }",
                description,
                true
            )
        }

        val connectedShards = shardCache.count { it.status == JDA.Status.CONNECTED }
        val avgGatewayPing = shardManager.averageGatewayPing.roundToInt()
        val avgRestPing = restPings.average().roundToInt()
        val guilds = shardManager.guildCache.size()

        embedBuilder.addField(
            "Total/Average", """
            - Connected: `$connectedShards`
            - Gateway Ping: `$avgGatewayPing`
            - REST Ping: `$avgRestPing`
            - Guilds: `$guilds`
        """.trimIndent(), false
        )

        event.replyEmbeds(embedBuilder.build())
            .setEphemeral(true)
            .queue()
    }

    private val JDA.shardStatus: String
        get() = this.status.toString().replace("_", " ").uppercase()

}