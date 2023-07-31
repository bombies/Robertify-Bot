package main.utils.pagination.pages.playlists

import main.utils.RobertifyEmbedUtils
import main.utils.api.robertify.imagebuilders.builders.playlists.PlaylistsImageBuilder
import main.utils.database.mongodb.databases.playlists.PlaylistModel
import main.utils.pagination.pages.AbstractImagePage
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import java.io.InputStream

class PlaylistsPage(
    private val guild: Guild,
    private val _playlists: List<PlaylistModel> = mutableListOf(),
    private val pageNumber: Int
) : AbstractImagePage() {
    override val embed: MessageEmbed
        get() {
            val content = _playlists.mapIndexed { i, playlist ->
                "**${i + 1}.** ${playlist.title} *(${playlist.tracks.size} tracks)*\n"
            }
            return RobertifyEmbedUtils.embedMessage(
                guild, "\t" +
                        "$content"
            ).build()
        }

    override suspend fun generateImage(): InputStream? = PlaylistsImageBuilder(
        guild = guild,
        page = pageNumber,
        playlists = _playlists
    ).build()
}