package main.utils.resume

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import main.audiohandlers.models.RequesterKt
import main.audiohandlers.sources.resume.ResumeSourceManagerKt
import main.audiohandlers.sources.resume.ResumeTrackKt
import main.utils.internal.jackson.DefaultObjectMapper
import org.json.JSONArray
import org.json.JSONObject

data class ResumableTrackKt(
    val info: AudioTrackInfoWrapperKt,
    val artworkURL: String?,
    val isrc: String?,
    val requester: RequesterKt?
) {
    companion object {
        val mapper = DefaultObjectMapper()

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
            mapper.readValue(json)
    }

    fun toAudioTrack(sourceManager: ResumeSourceManagerKt): AudioTrack =
        ResumeTrackKt(info, isrc, artworkURL, sourceManager)

    override fun toString(): String =
        mapper.writeValueAsString(this)
}
