package main.utils.api.robertify.imagebuilders.builders

import main.utils.GeneralUtils.isRightToLeft
import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageBuilderException
import main.utils.api.robertify.imagebuilders.ImageType
import main.utils.api.robertify.imagebuilders.models.ImageQueryField
import org.json.JSONObject
import java.io.InputStream

data class NowPlayingImageBuilder(
    private val artistName: String,
    private val title: String,
    private val albumImage: String,
    private val duration: Long? = null,
    private val currentTime: Long? = null,
    private val requesterName: String? = null,
    private val requesterAvatar: String? = null,
    private val isLiveStream: Boolean = false
) : AbstractImageBuilder(ImageType.NOW_PLAYING) {

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

    override suspend fun build(): InputStream? {
        if (artistName.isRightToLeft() ||
            artistName.isRightToLeft() ||
            requesterName.isRightToLeft()
        )
            throw ImageBuilderException("Some text has right to left characters which aren't supported!")

        return super.build()
    }

    private enum class QueryFields : ImageQueryField {
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
