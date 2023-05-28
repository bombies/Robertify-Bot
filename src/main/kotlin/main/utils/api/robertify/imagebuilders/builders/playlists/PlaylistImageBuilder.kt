package main.utils.api.robertify.imagebuilders.builders.playlists

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageType
import main.utils.api.robertify.imagebuilders.models.ImageQueryField
import main.utils.database.mongodb.databases.playlists.PlaylistTrack
import main.utils.json.themes.ThemesConfig
import net.dv8tion.jda.api.entities.Guild
import java.io.InputStream

class PlaylistImageBuilder(
    private val guild: Guild,
    private val title: String,
    private val artworkUrl: String,
    private val description: String,
    private val tracks: List<PlaylistTrack>,
    private val page: Int
) : AbstractImageBuilder(ImageType.PLAYLISTS_CONTENT) {

    override fun build(): InputStream? {
        val totalDuration = tracks.sumOf { it.duration }
        addQuery(QueryFields.TITLE, title)
        addQuery(QueryFields.DESCRIPTION, description)
        addQuery(QueryFields.ARTWORK_URL, artworkUrl)
        addQuery(QueryFields.TOTAL_DURATION, totalDuration.toString())
        addQuery(QueryFields.TRACK_COUNT, tracks.size.toString())
        addQuery(QueryFields.TRACKS, Json.encodeToString(tracks))
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
        PAGE,
        THEME;

        override fun toString(): String =
            name.lowercase()
    }
}