package main.utils.api.robertify.imagebuilders.builders

import main.utils.GeneralUtilsKt.isRightToLeft
import main.utils.api.robertify.imagebuilders.AbstractImageBuilderKt
import main.utils.api.robertify.imagebuilders.ImageBuilderExceptionKt
import main.utils.api.robertify.imagebuilders.ImageTypeKt
import main.utils.api.robertify.imagebuilders.models.ImageQueryFieldKt
import org.json.JSONObject
import java.io.InputStream

data class NowPlayingImageBuilderKt(
    private val artistName: String,
    private val title: String,
    private val albumImage: String,
    private val duration: Long? = null,
    private val currentTime: Long? = null,
    private val requesterName: String? = null,
    private val requesterAvatar: String? = null,
    private val isLiveStream: Boolean = false
) : AbstractImageBuilderKt(ImageTypeKt.NOW_PLAYING) {

    init {
        addQuery(QueryFields.ARTIST, artistName)
        addQuery(QueryFields.TITLE, title)
        addQuery(QueryFields.ALBUM_IMAGE, albumImage)

        if (duration != null)
            addQuery(QueryFields.DURATION, duration.toString())

        if (currentTime != null)
            addQuery(QueryFields.CURRENT_TIME, currentTime.toString())

        if (requesterName != null) {
            addQuery(
                QueryFields.REQUESTER, JSONObject()
                    .put(QueryFields.USER_NAME.toString(), requesterName)
                    .put(QueryFields.USER_IMAGE.toString(), requesterAvatar ?: "https://i.imgur.com/t0Y0EbT.png")
                    .toString()
            )
        }

        addQuery(QueryFields.LIVESTREAM, isLiveStream.toString())
    }

    override fun build(): InputStream? {
        if (artistName.isRightToLeft() ||
            artistName.isRightToLeft() ||
            requesterName.isRightToLeft()
        )
            throw ImageBuilderExceptionKt("Some text has right to left characters which aren't supported!")

        return super.build()
    }

    private enum class QueryFields : ImageQueryFieldKt {
        ARTIST,
        TITLE,
        ALBUM_IMAGE,
        DURATION,
        REQUESTER,
        USER_NAME,
        USER_IMAGE,
        CURRENT_TIME,
        LIVESTREAM;

        override fun toString(): String = name.lowercase()
    }
}
