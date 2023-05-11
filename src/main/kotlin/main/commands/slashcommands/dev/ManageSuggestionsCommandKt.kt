package main.commands.slashcommands.dev

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandGroupKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.GeneralUtilsKt.queueAfter
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.database.mongodb.cache.BotDBCacheKt
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.time.Duration.Companion.seconds

class ManageSuggestionsCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "suggestions",
        description = "Manage suggestions",
        developerOnly = true,
        subCommandGroups = listOf(
            SubCommandGroupKt(
                name = "set",
                description = "Modify the suggestion channels",
                subCommands = listOf(
                    SubCommandKt(
                        name = "accepted",
                        description = "Set the accepted suggestions channel",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The channel to set as the accepted suggestions channel"
                            )
                        )
                    ),
                    SubCommandKt(
                        name = "denied",
                        description = "Set the denied suggestions channel",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.CHANNEL,
                                channelTypes = listOf(ChannelType.TEXT),
                                name = "channel",
                                description = "The channel to set as the denied suggestions channel"
                            )
                        )
                    ),
                    SubCommandKt(
                        name = "pending",
                        description = "Set the pending suggestions channel",
                        options = listOf(
                            CommandOptionKt(
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
            SubCommandKt(
                name = "setup",
                description = "Setup the suggestions category."
            ),
            SubCommandKt(
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

    private fun handleChannelSet(event: SlashCommandInteractionEvent) {
        val (_, _, secondaryCommand) = event.fullCommandName.split(" ")
        val config = BotDBCacheKt.instance
        val guild = event.guild!!

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

    private fun handleChannelsSetup(event: SlashCommandInteractionEvent) {
        val config = BotDBCacheKt.instance
        val guild = event.guild!!

        event.deferReply(true).queue()

        if (config.suggestionSetup)
            return event.replyEmbed("The suggestions channels have already been setup!")
                .setEphemeral(true)
                .queue()

        guild.createCategory("Suggestions")
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
            .queue { category ->
                guild.createTextChannel("pending-suggestions", category)
                    .queueAfter(2.seconds) { pendingChannel ->
                        guild.createTextChannel("accepted-suggestions", category)
                            .queueAfter(1.seconds) { acceptedChannel ->
                                guild.createTextChannel("denied-suggestions", category)
                                    .queueAfter(1.seconds) { deniedChannel ->
                                        config.initSuggestionChannels(
                                            categoryID = category.idLong,
                                            pendingChannel = pendingChannel.idLong,
                                            acceptedChannel = acceptedChannel.idLong,
                                            deniedChannel = deniedChannel.idLong
                                        )
                                        event.hook.sendEmbed(guild, "Successfully setup the suggestion channels")
                                            .queue()
                                    }
                            }
                    }
            }
    }

    private fun handleReset(event: SlashCommandInteractionEvent) {
        val config = BotDBCacheKt.instance
        config.resetSuggestionsConfig()
        event.replyEmbed("Successfully reset the suggestions config")
            .setEphemeral(true)
            .queue()
    }

}