package main.utils.api.robertify.imagebuilders.builders

import dev.arbjerg.lavalink.protocol.v4.Track
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.utils.GeneralUtils.isRightToLeft
import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageBuilderException
import main.utils.api.robertify.imagebuilders.ImageType
import main.utils.api.robertify.imagebuilders.models.ImageQueryField
import main.utils.json.themes.ThemesConfig
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

data class QueueImageBuilder(
    private val guild: Guild,
    private val page: Int,
) : AbstractImageBuilder(ImageType.QUEUE) {

    private val obj = JSONObject()

    fun addTrack(index: Int, title: String, artist: String, duration: Long): QueueImageBuilder {
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

    fun addTrack(index: Int, track: Track): QueueImageBuilder =
        addTrack(index, track.title, track.author, track.length)

    override suspend fun build(): InputStream? {
        require(obj.has(QueryFields.TRACKS.toString())) { "The track list must be provided before building the queue image!" }
        obj.put(QueryFields.PAGE.toString(), page)

        val trackObj = obj.getJSONArray(QueryFields.TRACKS.toString())
        trackObj.forEach { obj ->
            if (obj is JSONObject) {
                val name = obj.getString(QueryFields.TRACK_NAME.toString())
                val artist = obj.getString(QueryFields.TRACK_ARTIST.toString())

                if (name.isRightToLeft() || artist.isRightToLeft())
                    throw ImageBuilderException("Some text has right to left characters which aren't supported!")
            }
        }

        addQuery(QueryFields.TRACKS, obj.toString())
        addQuery(QueryFields.THEME, ThemesConfig(guild).theme.name.lowercase())
        return super.build()
    }


    private enum class QueryFields : ImageQueryField {
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
