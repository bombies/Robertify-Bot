package main.utils.api.robertify.imagebuilders.builders.playlists

import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageType
import main.utils.api.robertify.imagebuilders.models.ImageQueryField
import main.utils.database.mongodb.databases.playlists.PlaylistModel
import main.utils.json.themes.ThemesConfig
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

class PlaylistsImageBuilder(
    private val guild: Guild,
    private val page: Int,
    private val playlists: List<PlaylistModel>
) : AbstractImageBuilder(ImageType.PLAYLISTS_LIST) {

    override suspend fun build(): InputStream? {
        require(playlists.isNotEmpty()) { "The list of playlists must not be empty!" }

        val arr = JSONArray()
        playlists.forEachIndexed { i, playlist ->
            arr.put(
                JSONObject()
                    .put(QueryFields.PLAYLIST_TITLE.toString(), playlist.title)
                    .put(QueryFields.PLAYLIST_TRACK_COUNT.toString(), playlist.tracks.size)
                    .put(QueryFields.PLAYLIST_ARTWORK_URL.toString(), playlist.artwork_url)
                    .put(QueryFields.PLAYLIST_INDEX.toString(), i)
            )
        }

        addQuery(QueryFields.PLAYLISTS, arr.toString())
        addQuery(QueryFields.PAGE, page.toString())
        addQuery(QueryFields.THEME, ThemesConfig(guild).theme.name.lowercase())
        return super.build()
    }

    private enum class QueryFields : ImageQueryField {
        PAGE,
        PLAYLISTS,
        PLAYLIST_TITLE,
        PLAYLIST_ARTWORK_URL,
        PLAYLIST_TRACK_COUNT,
        PLAYLIST_INDEX,
        THEME;

        override fun toString(): String =
            name.lowercase()
    }
}