package main.commands.slashcommands.management.logs

import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.logs.LogConfig
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.LogChannelMessages
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class SetLogChannelCommand : AbstractSlashCommand(
    SlashCommand(
        name = "setlogchannel",
        description = "Set a specific channel to be the channel where Robertify logs should be sent.",
        options = listOf(
            CommandOption(
                type = OptionType.CHANNEL,
                name = "channel",
                description = "The channel to be set as the log channel.",
                channelTypes = listOf(ChannelType.TEXT)
            )
        ),
        adminOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val channel = event.getRequiredOption("channel").asChannel.asGuildMessageChannel()

        val requestChannelConfig = RequestChannelConfig(guild)
        if (requestChannelConfig.isRequestChannel(channel)) {
            event.replyEmbed {
                embed(LogChannelMessages.CANNOT_SET_LOG_CHANNEL)
            }.setEphemeral(true)
                .queue()
            return
        }

        val config = LogConfig(guild)

        if (config.channelIsSet) {
            config.removeChannel()
        }

        config.channelId = channel.idLong
        event.replyEmbed {
            embed(
                LogChannelMessages.LOG_CHANNEL_SET,
                Pair("{channel}", channel.asMention)
            )
        }.queue()
    }

    override val help: String
        get() = "Run this command to set where logs should be sent!"
}