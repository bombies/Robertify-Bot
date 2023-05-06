package main.commands.slashcommands.dev

import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.math.roundToInt

class ShardInfoCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "shardinfo",
        description = "Get the information about all shards related to the bot.",
        developerOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val embedBuilder = RobertifyEmbedUtilsKt.getEmbedBuilder(guild)
        val currentShard = event.jda.shardInfo.shardId
        val shardManager = event.jda.shardManager!!
        val shardCache = shardManager.shardCache
        val shards = shardCache.asList().reversed()

        shards.forEach { shard ->
            val description = """
                **Status:** ${shard.shardStatus}
                **Ping:** ${shard.gatewayPing}
                **Guilds:** ${shard.guildCache.size()}
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
        val avgPing = shardManager.averageGatewayPing.roundToInt()
        val guilds = shardManager.guildCache.size()

        embedBuilder.addField("Total/Average", """
            **Connected:** $connectedShards
            **Ping:** $avgPing
            **Guilds:** $guilds
        """.trimIndent(), false)

        event.replyEmbeds(embedBuilder.build())
            .setEphemeral(true)
            .queue()
    }

    private val JDA.shardStatus: String
        get() = this.status.toString().replace("_", " ").uppercase()

}