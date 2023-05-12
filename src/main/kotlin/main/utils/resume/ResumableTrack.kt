package main.utils.resume

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import main.audiohandlers.models.Requester
import main.audiohandlers.sources.resume.ResumeSourceManager
import main.audiohandlers.sources.resume.ResumeTrack
import org.json.JSONArray
import org.json.JSONObject

@Serializable
data class ResumableTrack(
    val info: AudioTrackInfoWrapper,
    val artworkURL: String?,
    val isrc: String?,
    val requester: Requester?
) {
    companion object {
        fun Collection<Pair<AudioTrack, Requester>>.toResumableTracks(): List<ResumableTrack> =
            this.map { trackPair ->
                val track = trackPair.first
                val requester = trackPair.second
                val info = AudioTrackInfoWrapper(track.info)
                var artworkURL: String? = null
                var isrc: String? = null

                if (track is MirroringAudioTrack) {
                    artworkURL = track.artworkURL
                    isrc = track.isrc
                }

                ResumableTrack(info, artworkURL, isrc, requester)
            }

        fun Collection<ResumableTrack>.string(): String {
            val arr = JSONArray()
            this.forEach { track -> arr.put(JSONObject(track.toString())) }
            return arr.toString()
        }

        fun fromJSON(json: String): ResumableTrack =
            Json.decodeFromString(json)
    }

    constructor(track: AudioTrack, requester: Requester?) : this(
        info = AudioTrackInfoWrapper(track.info),
        requester = requester,
        artworkURL = if (track is MirroringAudioTrack) track.artworkURL else null,
        isrc = if (track is MirroringAudioTrack) track.isrc else null
    )

    fun toAudioTrack(sourceManager: ResumeSourceManager): AudioTrack =
        ResumeTrack(info.toAudioTrackInfo(), isrc, artworkURL, sourceManager)

    override fun toString(): String =
        Json.encodeToString(this)
}
