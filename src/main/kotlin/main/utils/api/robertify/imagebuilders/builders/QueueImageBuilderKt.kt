package main.utils.api.robertify.imagebuilders.builders

import dev.schlaubi.lavakord.audio.player.Track
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.Companion.isRightToLeft
import main.utils.api.robertify.imagebuilders.AbstractImageBuilderKt
import main.utils.api.robertify.imagebuilders.ImageBuilderExceptionKt
import main.utils.api.robertify.imagebuilders.ImageTypeKt
import main.utils.api.robertify.imagebuilders.models.ImageQueryFieldKt
import main.utils.json.themes.ThemesConfigKt
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

data class QueueImageBuilderKt(
    private val guild: Guild,
    private val page: Int,
) : AbstractImageBuilderKt(ImageTypeKt.QUEUE) {

    private val obj = JSONObject()

    fun addTrack(index: Int, title: String, artist: String, duration: Long): QueueImageBuilderKt {
        if (!obj.has(QueryFields.TRACKS.toString()))
            obj.put(QueryFields.TRACKS.toString(), JSONArray())

        val trackArr = obj.getJSONArray(QueryFields.TRACKS.toString())
        val trackObj = JSONObject()
            .put(QueryFields.TRACK_INDEX.toString(), index)
            .put(QueryFields.TRACK_NAME.toString(), title)
            .put(QueryFields.TRACK_ARTIST.toString(), artist)
            .put(QueryFields.TRACK_DURATION.toString(), duration);
        trackArr.put(trackObj)
        return this
    }

    fun addTrack(index: Int, track: Track): QueueImageBuilderKt =
        addTrack(index, track.title, track.author, track.length.inWholeMilliseconds)

    override fun build(): InputStream? {
        require(obj.has(QueryFields.TRACKS.toString())) { "The track list must be provided before building the queue image!" }
        obj.put(QueryFields.PAGE.toString(), page)

        val trackObj = obj.getJSONArray(QueryFields.TRACKS.toString())
        trackObj.forEach { obj ->
            if (obj is JSONObject) {
                val name = obj.getString(QueryFields.TRACK_NAME.toString())
                val artist = obj.getString(QueryFields.TRACK_ARTIST.toString())

                if (name.isRightToLeft() || artist.isRightToLeft())
                    throw ImageBuilderExceptionKt("Some text has right to left characters which aren't supported!")
            }
        }

        addQuery(QueryFields.TRACKS, obj.toString())
        addQuery(QueryFields.THEME, ThemesConfigKt(guild).theme.name.lowercase())
        return super.build()
    }


    private enum class QueryFields : ImageQueryFieldKt {
        PAGE,
        TRACKS,
        TRACK_INDEX,
        TRACK_NAME,
        TRACK_ARTIST,
        TRACK_DURATION,
        THEME;

        override fun toString(): String =
            name.lowercase()
    }

}
