//package main.utils.resume
//
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.encodeToString
//import kotlinx.serialization.json.Json
//
//@Serializable
//data class ResumeData(val channel_id: String, val tracks: List<ResumableTrack>) {
//
//    companion object {
//        fun fromJSON(json: String): ResumeData =
//            Json.decodeFromString(json)
//    }
//
//    override fun toString(): String =
//        Json.encodeToString(this)
//}
