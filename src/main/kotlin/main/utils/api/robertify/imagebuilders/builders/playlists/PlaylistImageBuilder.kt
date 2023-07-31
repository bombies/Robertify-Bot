package main.utils.api.robertify.imagebuilders.builders.playlists

import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageType
import main.utils.api.robertify.imagebuilders.models.ImageQueryField
import main.utils.database.mongodb.databases.playlists.PlaylistTrack
import main.utils.json.themes.ThemesConfig
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

class PlaylistImageBuilder(
    private val guild: Guild,
    private val title: String,
    private val artworkUrl: String,
    private val description: String,
    private val tracks: List<PlaylistTrack>,
    private val trackIndexes: Map<PlaylistTrack, Int>,
    private val page: Int,
    private val sortingOrder: PlaylistSortingOrder = PlaylistSortingOrder.NONE
) : AbstractImageBuilder(ImageType.PLAYLISTS_CONTENT) {

    override suspend fun build(): InputStream? {
        val totalDuration = tracks.sumOf { it.duration }
        addQuery(QueryFields.TITLE, title)
        addQuery(QueryFields.DESCRIPTION, description)
        addQuery(QueryFields.ARTWORK_URL, artworkUrl)
        addQuery(QueryFields.TOTAL_DURATION, totalDuration.toString())
        addQuery(QueryFields.TRACK_COUNT, tracks.size.toString())

        val tracksArray = JSONArray()
        tracks.forEach { playlistTrack ->
            tracksArray.put(
                JSONObject()
                    .put(QueryFields.TRACK_TITLE.toString(), playlistTrack.title)
                    .put(QueryFields.TRACK_AUTHOR.toString(), playlistTrack.author)
                    .put(QueryFields.TRACK_DURATION.toString(), playlistTrack.duration.toString())
                    .put(QueryFields.TRACK_IDENTIFIER.toString(), playlistTrack.identifier)
                    .put(QueryFields.TRACK_INDEX.toString(), trackIndexes[playlistTrack])
            )
        }

        addQuery(QueryFields.TRACKS, tracksArray.toString())
        addQuery(QueryFields.SORT_BY, sortingOrder.ordinal.toString())
        addQuery(QueryFields.PAGE, page.toString())
        addQuery(QueryFields.THEME, ThemesConfig(guild).theme.name.lowercase())
        return super.build()
    }

    private enum class QueryFields : ImageQueryField {
        TITLE,
        DESCRIPTION,
        ARTWORK_URL,
        TRACK_COUNT,
        TOTAL_DURATION,
        TRACKS,
        TRACK_TITLE,
        TRACK_AUTHOR,
        TRACK_DURATION,
        TRACK_IDENTIFIER,
        TRACK_INDEX,
        SORT_BY,
        PAGE,
        THEME;

        override fun toString(): String =
            name.lowercase()
    }

    enum class PlaylistSortingOrder {
        ASCENDING_DATE_ADDED,
        DESCENDING_DATE_ADDED,
        ASCENDING_NAME,
        ASCENDING_ARTIST,
        DESCENDING_NAME,
        DESCENDING_ARTIST,
        NONE
    }
}