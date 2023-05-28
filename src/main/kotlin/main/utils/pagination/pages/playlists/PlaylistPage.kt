package main.utils.pagination.pages.playlists

import main.utils.GeneralUtils.coerceAtMost
import main.utils.RobertifyEmbedUtils
import main.utils.api.robertify.imagebuilders.builders.playlists.PlaylistImageBuilder
import main.utils.database.mongodb.databases.playlists.PlaylistTrack
import main.utils.pagination.pages.AbstractImagePage
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import java.io.InputStream

class PlaylistPage(
    private val title: String,
    private val description: String,
    private val artworkUrl: String,
    private val guild: Guild,
    private val tracks: List<PlaylistTrack>,
    pageNumber: Int
) : AbstractImagePage() {
    override val embed: MessageEmbed
        get() {
        val content = tracks.mapIndexed { i, track ->
            "**${i + 1}.** ${track.title.coerceAtMost(50)} by ${track.author.coerceAtMost(50)}\n"
        }
        return RobertifyEmbedUtils.embedMessage(
            guild, "# $title\n### $description\n" +
                    "$content"
        )
            .setThumbnail(artworkUrl)
            .build()
    }

    override val image: InputStream? = PlaylistImageBuilder(
        guild = guild,
        title = title,
        artworkUrl = artworkUrl,
        description = description,
        page = pageNumber,
        tracks = tracks
    ).build()
}