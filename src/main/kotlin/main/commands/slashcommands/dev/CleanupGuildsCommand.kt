package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import main.main.Config
import main.main.Robertify
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.database.mongodb.databases.GuildDB
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class CleanupGuildsCommand : AbstractSlashCommand(
    SlashCommand(
        name = "cleanupguilds",
        description = "Cleanup any guild documents that don't correspond to the bots current guilds",
        developerOnly = true
    )
) {

    companion object {
        val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()

        if (Robertify.shardManager.shardsRunning != Config.SHARD_COUNT) {
            event.hook.sendEmbed(message = "Please wait until all shards are ready before running this command!")
                .queue()
            return
        }

        GuildDB.cleanup()
        event.hook.sendEmbed(message = "Successfully cleaned up guilds!").queue()
    }

}