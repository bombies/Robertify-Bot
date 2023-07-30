//package main.utils.resume
//
//import dev.arbjerg.lavalink.protocol.v4.Track
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.encodeToString
//import kotlinx.serialization.json.Json
//import main.audiohandlers.models.Requester
//import main.audiohandlers.utils.artworkUrl
//import main.audiohandlers.utils.isrc
//import org.json.JSONArray
//import org.json.JSONObject
//
//@Serializable
//data class ResumableTrack(
//    val info: AudioTrackInfoWrapper,
//    val artworkURL: String?,
//    val isrc: String?,
//    val requester: Requester?
//) {
//    companion object {
//        fun Collection<Pair<Track, Requester>>.toResumableTracks(): List<ResumableTrack> =
//            this.map { trackPair ->
//                val track = trackPair.first
//                val requester = trackPair.second
//                val info = AudioTrackInfoWrapper(track.info)
//                val artworkURL = track.artworkUrl
//                val isrc = track.isrc
//
//                ResumableTrack(info, artworkURL, isrc, requester)
//            }
//
//        fun Collection<ResumableTrack>.string(): String {
//            val arr = JSONArray()
//            this.forEach { track -> arr.put(JSONObject(track.toString())) }
//            return arr.toString()
//        }
//
//        fun fromJSON(json: String): ResumableTrack =
//            Json.decodeFromString(json)
//    }
//
//    constructor(track: Track, requester: Requester?) : this(
//        info = AudioTrackInfoWrapper(track.info),
//        requester = requester,
//        artworkURL = track.artworkUrl,
//        isrc = track.isrc
//    )
//
//    fun toAudioTrack(sourceManager: ResumeSourceManager): Track =
//        ResumeTrack(info.toAudioTrackInfo(), isrc, artworkURL, sourceManager)
//
//    override fun toString(): String =
//        Json.encodeToString(this)
//}
