package main.utils.resume

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import main.audiohandlers.models.RequesterKt
import main.audiohandlers.sources.resume.ResumeSourceManagerKt
import main.audiohandlers.sources.resume.ResumeTrackKt
import org.json.JSONArray
import org.json.JSONObject

@Serializable
data class ResumableTrackKt(
    val info: AudioTrackInfoWrapperKt,
    val artworkURL: String?,
    val isrc: String?,
    val requester: RequesterKt?
) {
    companion object {
        fun Collection<Pair<AudioTrack, RequesterKt>>.toResumableTracks(): List<ResumableTrackKt> =
            this.map { trackPair ->
                val track = trackPair.first
                val requester = trackPair.second
                val info = AudioTrackInfoWrapperKt(track.info)
                var artworkURL: String? = null
                var isrc: String? = null

                if (track is MirroringAudioTrack) {
                    artworkURL = track.artworkURL
                    isrc = track.isrc
                }

                ResumableTrackKt(info, artworkURL, isrc, requester)
            }

        fun Collection<ResumableTrackKt>.string() {
            val arr = JSONArray()
            this.forEach { track -> arr.put(JSONObject(track.toString())) }
        }

        fun fromJSON(json: String): ResumableTrackKt =
            Json.decodeFromString(json)
    }

    constructor(track: AudioTrack, requester: RequesterKt?) : this(
        info = AudioTrackInfoWrapperKt(track.info),
        requester = requester,
        artworkURL = if (track is MirroringAudioTrack) track.artworkURL else null,
        isrc = if (track is MirroringAudioTrack) track.isrc else null
    )

    fun toAudioTrack(sourceManager: ResumeSourceManagerKt): AudioTrack =
        ResumeTrackKt(info.toAudioTrackInfo(), isrc, artworkURL, sourceManager)

    override fun toString(): String =
        Json.encodeToString(this)
}
