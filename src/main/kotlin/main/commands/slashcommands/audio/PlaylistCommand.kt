package main.commands.slashcommands.audio

import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.database.mongodb.databases.playlists.PlaylistDB
import main.utils.locale.messages.PlaylistMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PlaylistCommand : AbstractSlashCommand(SlashCommand(
    name = "playlists",
    description = "Interact with your playlists!.",
    subcommands = listOf(
        SubCommand(
            name = "list",
            description = "List all your playlists."
        ),
        SubCommand(
            name = "create",
            description = "Create a new playlist."
        ),
        SubCommand(
            name = "delete",
            description = "Delete a playlist.",
            options = listOf(
                CommandOption(
                    name = "id",
                    description = "The ID of the playlist to remove",
                    autoComplete = true
                )
            )
        ),
        SubCommand(
            name = "edit",
            description = "Edit a playlist.",
            options = listOf(
                CommandOption(
                    name = "id",
                    description = "The ID of the playlist to remove"
                )
            )
        )
    )
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val (_, primaryCommand) = event.fullCommandName.split(" ")
        when (primaryCommand) {
            "list" -> {}
            "create" -> {}
            "delete" -> {}
            "update" -> {}
        }
    }

    private fun listPlaylists(event: SlashCommandInteractionEvent) {
        val playlists = PlaylistDB.findPlaylistForUser(event.user.id)
        return if (playlists.isEmpty())
            event.replyEmbed(PlaylistMessages.NO_PLAYLISTS).queue()
        else {

        }
    }
}