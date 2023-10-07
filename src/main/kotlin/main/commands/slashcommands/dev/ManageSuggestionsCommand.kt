package main.commands.slashcommands.dev

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.delay
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SubCommandGroup
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.GeneralUtils.queueAfter
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.database.mongodb.cache.BotDBCache
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.Duration.Companion.seconds

class ManageSuggestionsCommand : AbstractSlashCommand(
    SlashCommand(
        name = "suggestions",
        description = "Manage suggestions",
        developerOnly = true,
        subCommandGroups = listOf(
            SubCommandGroup(
                name = "set",
                description = "Modify the suggestion channels",
                subCommands = listOf(
                    SubCommand(
                        name = "accepted",
                        description = "Set the accepted suggestions channel",
                        options = listOf(
                            CommandOption(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The channel to set as the accepted suggestions channel"
                            )
                        )
                    ),
                    SubCommand(
                        name = "denied",
                        description = "Set the denied suggestions channel",
                        options = listOf(
                            CommandOption(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The channel to set as the denied suggestions channel"
                            )
                        )
                    ),
                    SubCommand(
                        name = "pending",
                        description = "Set the pending suggestions channel",
                        options = listOf(
                            CommandOption(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The channel to set as the pending suggestions channel"
                            )
                        )
                    ),
                )
            )
        ),
        subcommands = listOf(
            SubCommand(
                name = "setup",
                description = "Setup the suggestions category."
            ),
            SubCommand(
                name = "reset",
                description = "Reset the suggestions configuration."
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val (_, primaryCommand) = event.fullCommandName.split(" ")

        when (primaryCommand) {
            "set" -> handleChannelSet(event)
            "setup" -> handleChannelsSetup(event)
            "reset" -> handleReset(event)
        }
    }

    private suspend fun handleChannelSet(event: SlashCommandInteractionEvent) {
        val (_, _, secondaryCommand) = event.fullCommandName.split(" ")
        val config = BotDBCache.instance

        if (!config.suggestionSetup)
            return event.replyEmbed("The suggestions category must be setup before changing these channels!")
                .setEphemeral(true)
                .queue()

        val channel = event.getRequiredOption("channel").asChannel.asTextChannel()
        val currentPending = config.suggestionsPendingChannelId
        val currentAccepted = config.suggestionsAcceptedChannelId
        val currentDenied = config.suggestionsDeniedChannelId

        when (secondaryCommand) {
            "accepted" -> {
                if (channel.idLong == currentAccepted)
                    return event.replyEmbed("That is already the accepted channel!")
                        .setEphemeral(true)
                        .queue()

                if (channel.idLong == currentPending || channel.idLong == currentDenied)
                    return event.replyEmbed("You may not set the accepted channel to that channel!")
                        .setEphemeral(true)
                        .queue()

                config.suggestionsAcceptedChannelId = channel.idLong
                event.replyEmbed(
                    "You have successfully set the accepted suggestions channel to ${channel.asMention}"
                )
                    .setEphemeral(true)
                    .queue()
            }

            "denied" -> {
                if (channel.idLong == currentDenied)
                    return event.replyEmbed("That is already the denied channel!")
                        .setEphemeral(true)
                        .queue()

                if (channel.idLong == currentAccepted || channel.idLong == currentPending)
                    return event.replyEmbed("You may not set the denied channel to that channel!")
                        .setEphemeral(true)
                        .queue()

                config.suggestionsDeniedChannelId = channel.idLong
                event.replyEmbed(
                    "You have successfully set the denied suggestions channel to ${channel.asMention}"
                )
                    .setEphemeral(true)
                    .queue()
            }

            "pending" -> {
                if (channel.idLong == currentPending)
                    return event.replyEmbed("That is already the pending channel!")
                        .setEphemeral(true)
                        .queue()

                if (channel.idLong == currentAccepted || channel.idLong == currentDenied)
                    return event.replyEmbed("You may not set the pending channel to that channel!")
                        .setEphemeral(true)
                        .queue()

                config.suggestionsPendingChannelId = channel.idLong
                event.replyEmbed(
                    "You have successfully set the pending suggestions channel to ${channel.asMention}"
                )
                    .setEphemeral(true)
                    .queue()
            }
        }
    }

    private suspend fun handleChannelsSetup(event: SlashCommandInteractionEvent) {
        val config = BotDBCache.instance
        val guild = event.guild!!

        event.deferReply(true).queue()

        if (config.suggestionSetup)
            return event.hook.sendEmbed(guild, "The suggestions channels have already been setup!")
                .setEphemeral(true)
                .queue()

        val category = guild.createCategory("Suggestions")
            .addMemberPermissionOverride(
                guild.selfMember.idLong,
                listOf(Permission.VIEW_CHANNEL),
                listOf()
            )
            .addRolePermissionOverride(
                guild.publicRole.idLong,
                listOf(),
                listOf(Permission.VIEW_CHANNEL)
            )
            .await()

        delay(2.seconds)
        val pendingChannel = guild.createTextChannel("pending-suggestions", category)
            .await()

        delay(1.seconds)
        val acceptedChannel = guild.createTextChannel("accepted-suggestions", category)
            .await()

        delay(1.seconds)
        val deniedChannel = guild.createTextChannel("denied-suggestions", category)
            .await()

        config.initSuggestionChannels(
            categoryID = category.idLong,
            pendingChannel = pendingChannel.idLong,
            acceptedChannel = acceptedChannel.idLong,
            deniedChannel = deniedChannel.idLong
        )
        event.hook.sendEmbed(guild, "Successfully setup the suggestion channels")
            .queue()
    }

    private suspend fun handleReset(event: SlashCommandInteractionEvent) {
        val config = BotDBCache.instance
        config.resetSuggestionsConfig()
        event.replyEmbed("Successfully reset the suggestions config")
            .setEphemeral(true)
            .queue()
    }

}