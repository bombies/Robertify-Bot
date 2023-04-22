package main.utils.resume

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import kotlinx.serialization.Serializable

@Serializable
data class AudioTrackInfoWrapperKt(
    val title: String,
    val author: String,
    val length: Long,
    val identifier: String,
    val isStream: Boolean,
    val uri: String
) {
    constructor (info: AudioTrackInfo) : this(
        info.title,
        info.author,
        info.length,
        info.identifier,
        info.isStream,
        info.uri
    )

    fun toAudioTrackInfo(): AudioTrackInfo =
        AudioTrackInfo(
            title,
            author,
            length,
            identifier,
            isStream,
            uri
        )
}

