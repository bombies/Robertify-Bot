package main.utils.database.mongodb.databases.playlists

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PlaylistModel(
    val id: String,
    val owner: String,
    val artwork_url: String,
    val title: String,
    val description: String,
    val tracks: List<PlaylistTrack>
) {

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<PlaylistModel>(json)
    }

    fun toJson() = Json.encodeToString(this)
}

@Serializable
data class PlaylistTrack(
    val identifier: String,
    val title: String,
    val author: String,
    val duration: Long,
    val artwork_url: String,
    val isrc: String,
    val date_added: Long
) {

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<PlaylistTrack>(json)
    }

    fun toJson() = Json.encodeToString(this)
}

data class UpdatePlaylistModel(
    val name: String? = null,
    val description: String? = null,
    val artwork_url: String? = null
)
