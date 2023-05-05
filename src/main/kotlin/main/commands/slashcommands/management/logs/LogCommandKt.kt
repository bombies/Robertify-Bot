package main.commands.slashcommands.management.logs

import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.LogChannelMessages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class LogCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "setuplogs",
        description = "Setup the log channel for all Robertify player events.",
        adminOnly = true,
        botRequiredPermissions = listOf(Permission.MANAGE_CHANNEL, Permission.MESSAGE_MANAGE)
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!

        try {
            LogUtilsKt(guild).createChannel()
            event.replyWithEmbed(guild) {
                embed(LogChannelMessages.LOG_CHANNEL_SUCCESSFUL_SETUP)
            }.setEphemeral(true)
                .queue()
        } catch (e: IllegalArgumentException) {
            event.replyWithEmbed(guild) {
                embed(LogChannelMessages.LOG_CHANNEL_ALREADY_SETUP)
            }
                .setEphemeral(true)
                .queue()
        }
    }

    override val help: String
        get() = "Want to see every single action that's executed by users with regard to the music player?" +
                " This is the perfect command for you. Upon execution of this command, a log channel will " +
                "be created and all player updates will be sent to that channel which states the user who caused" +
                " the update. If you would like to remove this channel all you have to do is right click it and press " +
                "delete."
}