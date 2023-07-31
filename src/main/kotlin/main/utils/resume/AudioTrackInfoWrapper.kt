package main.utils.resume

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import dev.arbjerg.lavalink.protocol.v4.TrackInfo
import kotlinx.serialization.Serializable

@Serializable
data class AudioTrackInfoWrapper(
    val identifier: String,
    val isSeekable: Boolean,
    val author: String,
    val length: Long,
    val isStream: Boolean,
    val position: Long,
    val title: String,
    val uri: String?,
    val sourceName: String,
    val artworkUrl: String?,
    val isrc: String?
) {
    constructor (info: TrackInfo) : this(
        info.identifier,
        info.isSeekable,
        info.author,
        info.length,
        info.isStream,
        info.position,
        info.title,
        info.uri,
        info.sourceName,
        info.artworkUrl,
        info.isrc
    )

    fun toAudioTrackInfo(): TrackInfo =
        TrackInfo(
            identifier,
            isSeekable,
            author,
            length,
            isStream,
            position,
            title,
            uri,
            sourceName,
            artworkUrl,
            isrc
        )
}

