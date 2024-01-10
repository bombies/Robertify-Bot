package main.commands.slashcommands.dev

import dev.minn.jda.ktx.util.SLF4J
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.SubCommandGroup
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.json.requestchannel.RequestChannelConfig
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class UpdateCommand : AbstractSlashCommand(
    SlashCommand(
        name = "update",
        description = "Bring certain things up to date on the bot.",
        developerOnly = true,
        subcommands = listOf(
            SubCommand(
                name = "caches",
                description = "Update caches"
            )
        ),
        subCommandGroups = listOf(
            SubCommandGroup(
                name = "requestchannel",
                description = "Update all request channels",
                subCommands = listOf(
                    SubCommand(
                        name = "all",
                        description = "update all"
                    ),
                    SubCommand(
                        name = "buttons",
                        description = "Update all request channel buttons"
                    ),
                    SubCommand(
                        name = "message",
                        description = "Update all request channel messages"
                    ),
                    SubCommand(
                        name = "topic",
                        description = "Update all request channel topics"
                    ),
                    SubCommand(
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

    private suspend fun handleCacheUpdate(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()
        AbstractMongoDatabase.initAllCaches()
        event.hook.sendEmbed(event.guild, "Updated all caches!").queue()
    }

    private suspend fun handleRequestChannelUpdate(event: SlashCommandInteractionEvent) {
        val (_, _, secondaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (secondaryCommand) {
            "all" -> updateAllRequestChannels(event)
            "buttons" -> updateRequestChannelButtons(event)
            "message" -> updateRequestChannelMessages(event)
            "topic" -> updateRequestChannelTopics(event)
            "clean" -> cleanRequestChannels(event)
        }
    }

    private suspend fun updateAllRequestChannels(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channels!",
            errorMsg = "Could not update all request channels!"
        ) {
            it.updateAll()
        }
    }

    private suspend fun updateRequestChannelButtons(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channel buttons!",
            errorMsg = "Could not update all request channel buttons!"
        ) {
            it.updateButtons()
        }
    }

    private suspend fun updateRequestChannelMessages(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channel messages!",
            errorMsg = "Could not update all request channel messages!"
        ) {
            it.updateMessage()
        }
    }

    private suspend fun updateRequestChannelTopics(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully updated all request channel topics!",
            errorMsg = "Could not update all request channel topics!"
        ) {
            it.updateTopic()
        }
    }

    private suspend fun cleanRequestChannels(event: SlashCommandInteractionEvent) {
        handleGenericUpdate(
            event = event,
            successMsg = "Successfully cleaned all request channels!",
            errorMsg = "Could not clean all request channels!"
        ) {
            it.cleanChannel()
        }
    }

    private suspend inline fun handleGenericUpdate(
        event: SlashCommandInteractionEvent,
        successMsg: String,
        errorMsg: String,
        handler: (config: RequestChannelConfig) -> Unit
    ) {
        event.deferReply(true).queue()

        try {
            event.jda.shardManager!!.guildCache.forEach { guild ->
                val config = RequestChannelConfig(guild)
                handler(config)
            }

            event.hook.sendEmbed(event.guild, successMsg).queue()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            event.hook.sendEmbed(event.guild, errorMsg).queue()
        }
    }
}