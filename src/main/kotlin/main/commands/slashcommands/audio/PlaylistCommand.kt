package main.commands.slashcommands.audio

import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.TextInput
import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManager.getRequiredValue
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.database.mongodb.databases.playlists.PlaylistDB
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PlaylistMessages
import main.utils.pagination.PaginationHandler.paginatePlaylists
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

class PlaylistCommand : AbstractSlashCommand(
    SlashCommand(
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
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val (_, primaryCommand) = event.fullCommandName.split(" ")
        when (primaryCommand) {
            "list" -> listPlaylists(event)
            "create" -> {}
            "delete" -> {}
            "update" -> {}
        }
    }

    private suspend fun listPlaylists(event: SlashCommandInteractionEvent) {
        val playlists = PlaylistDB.findPlaylistForUser(event.user.id)
        return if (playlists.isEmpty())
            event.replyEmbed(PlaylistMessages.NO_PLAYLISTS).queue()
        else {
            paginatePlaylists(event, 25)
            return
        }
    }

    private fun createPlaylist(event: SlashCommandInteractionEvent) {
        val titleInputField = TextInput(
            id = "playlists:create:title",
            label = "Title",
            placeholder = "Enter the title of your playlist",
            style = TextInputStyle.SHORT,
            requiredLength = 1..25,
            required = true
        )

        val descInputField = TextInput(
            id = "playlists:create:description",
            label = "Description",
            placeholder = "Enter the description of your playlist",
            style = TextInputStyle.PARAGRAPH,
            requiredLength = 1..25,
            required = true
        )

        val artworkUrl = TextInput(
            id = "playlists:create:artwork",
            label = "Playlist Image URL",
            placeholder = "Enter the URL to an image that you would like to be displayed for your playlist",
            style = TextInputStyle.SHORT,
            required = false
        )

        val modal = Modal(
            id = "playlists:modal",
            title = "Create a Playlist",
            components = listOf(
                ActionRow.of(titleInputField),
                ActionRow.of(descInputField),
                ActionRow.of(artworkUrl),
            )
        )

        event.replyModal(modal).queue()
    }

    override suspend fun onModalInteraction(event: ModalInteractionEvent) {
        if (!event.modalId.startsWith("playlists")) return;

        val (_, modalType) = event.modalId.split(":");

        when (modalType) {
            "create" -> {
                val title = event.getRequiredValue("playlists:create:title").asString
                val desc = event.getRequiredValue("playlists:create:description").asString
                val artworkUrl = event.getValue("playlists:create:artwork")?.asString

                try {
                    PlaylistDB.createPlaylist(
                        userId = event.user.id,
                        name = title,
                        description = desc,
                        artworkUrl = artworkUrl
                    )

                    event.replyEmbed(PlaylistMessages.PLAYLIST_CREATED, Pair("{name}", title))
                        .setEphemeral(true)
                        .queue()
                } catch (e: Exception) {
                    logger.error("Unexpected error", e)
                    event.replyEmbed(GeneralMessages.UNEXPECTED_ERROR)
                        .setEphemeral(true)
                        .queue()
                }
            }
        }

    }
}