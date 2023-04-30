package main.commands.slashcommands.management

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.logs.LogConfigKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class SetLogChannelCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "setlogchannel",
        description = "Set a specific channel to be the channel where Robertify logs should be sent.",
        options = listOf(
            CommandOptionKt(
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

        val requestChannelConfig = RequestChannelConfigKt(guild)
        if (requestChannelConfig.isRequestChannel(channel)) {
            event.replyWithEmbed(guild) {
                embed(RobertifyLocaleMessageKt.LogChannelMessages.CANNOT_SET_LOG_CHANNEL)
            }.setEphemeral(true)
                .queue()
            return
        }

        val config = LogConfigKt(guild)

        if (config.channelIsSet) {
            config.removeChannel()
        }

        config.channelId = channel.idLong
        event.replyWithEmbed(guild) {
            embed(
                RobertifyLocaleMessageKt.LogChannelMessages.LOG_CHANNEL_SET,
                Pair("{channel}", channel.asMention)
            )
        }.queue()
    }

    override val help: String
        get() = "Run this command to set where logs should be sent!"
}