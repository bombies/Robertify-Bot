package main.commands.slashcommands.management.requestchannel

import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class RequestChannelCommand : AbstractSlashCommand(
    SlashCommand(
        name = "setup",
        description = "Create the easy-to-use request channel.",
        adminOnly = true,
        botRequiredPermissions = listOf(Permission.MESSAGE_MANAGE, Permission.MANAGE_CHANNEL)
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) =
        RequestChannelEditCommand().handleSetup(event)

    override val help: String
        get() = "Running this command will build a channel in which you can easily control the bot and" +
                " queue songs. When this channel is created, if you want it removed all you have to do" +
                " is right click on it and delete it. Once the channel is created you can find it at the" +
                " top of your channel list. Happy listening!"
}