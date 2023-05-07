package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.SubCommandGroupKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class UpdateCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "update",
        description = "Bring certain things up to date on the bot.",
        developerOnly = true,
        subcommands = listOf(
            SubCommandKt(
                name = "caches",
                description = "Update caches"
            )
        ),
        subCommandGroups = listOf(
            SubCommandGroupKt(
                name = "requestchannel",
                description = "Update all request channels",
                subCommands = listOf(
                    SubCommandKt(
                        name = "all",
                        description = "update all"
                    ),
                    SubCommandKt(
                        name = "buttons",
                        description = "Update all request channel buttons"
                    ),
                    SubCommandKt(
                        name = "message",
                        description = "Update all request channel messages"
                    ),
                    SubCommandKt(
                        name = "topic",
                        description = "Update all request channel topics"
                    ),
                    SubCommandKt(
                        name = "clean",
                        description = "Clean all request channels"
                    )
                )
            )
        )
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val (_, primaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (primaryCommand) {
            "caches" -> handleCacheUpdate(event)
            "requestchannel" -> handleRequestChannelUpdate(event)
        }
    }

    private fun handleCacheUpdate(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()
        AbstractMongoDatabaseKt.initAllCaches()
        event.hook.sendEmbed(event.guild, "Updated all caches!").queue()
    }

    private fun handleRequestChannelUpdate(event: SlashCommandInteractionEvent) {
        val (_, _, secondaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (secondaryCommand) {
            "all" -> updateAllRequestChannels(event)
            "buttons" -> updateRequestChannelButtons(event)
            "message" -> updateRequestChannelMessages(event)
            "topic" -> updateRequestChannelTopics(event)
            "clean" -> cleanRequestChannels(event)
        }
    }

    private fun updateAllRequestChannels(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channels!",
            errorMsg = "Could not update all request channels!"
        ) {
            it.updateAll()
        }
    }

    private fun updateRequestChannelButtons(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channel buttons!",
            errorMsg = "Could not update all request channel buttons!"
        ) {
            it.updateButtons()?.join()
        }
    }

    private fun updateRequestChannelMessages(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channel messages!",
            errorMsg = "Could not update all request channel messages!"
        ) {
            it.updateMessage()?.join()
        }
    }

    private fun updateRequestChannelTopics(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channel topics!",
            errorMsg = "Could not update all request channel topics!"
        ) {
            it.updateTopic()?.join()
        }
    }

    private fun cleanRequestChannels(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully cleaned all request channels!",
            errorMsg = "Could not clean all request channels!"
        ) {
            it.cleanChannel()
        }
    }

    private inline fun handleGenericUpdate(
        event: SlashCommandInteractionEvent,
        successMsg: String,
        errorMsg: String,
        handler: (config: RequestChannelConfigKt) -> Unit
    ) {
        event.deferReply(true).queue()

        try {
            event.jda.shardManager!!.guildCache.forEach { guild ->
                val config = RequestChannelConfigKt(guild)
                handler(config)
            }

            event.hook.sendEmbed(event.guild, successMsg).queue()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            event.hook.sendEmbed(event.guild, errorMsg).queue()
        }
    }
}