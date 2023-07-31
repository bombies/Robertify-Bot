package main.utils.database.mongodb.databases.playlists

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import main.constants.database.RobertifyMongoDatabase
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.database.mongodb.DocumentBuilder
import main.utils.json.GenericJSONField
import main.utils.json.put
import main.utils.json.remove
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PlaylistDB : AbstractMongoDatabase(collection = RobertifyMongoDatabase.ROBERTIFY_PLAYLISTS) {

    fun createPlaylist(
        userId: String,
        name: String,
        description: String? = null,
        artworkUrl: String? = null
    ) {
        val playlistDocument = DocumentBuilder.create()
            .addField(Fields.ID, UUID.randomUUID())
            .addField(Fields.OWNER, userId)
            .addField(Fields.TITLE, name)
            .addField(Fields.DESCRIPTION, description ?: "")
            .addField(Fields.ARTWORK_URL, artworkUrl ?: "")
            .addField(Fields.TRACKS, JSONArray())
            .build()
        addDocument(playlistDocument)
    }

    fun findPlaylist(uuid: String): PlaylistModel? {
        val playlistDocument = getDocument(Fields.ID.toString(), uuid) ?: return null
        return Json.decodeFromString<PlaylistModel>(playlistDocument)
    }

    fun findPlaylistForUser(userId: String): List<PlaylistModel> =
        getDocuments(Fields.OWNER.toString(), userId)
            .split("\n")
            .map { Json.decodeFromString<PlaylistModel>(it) }

    fun deletePlaylist(uuid: String) {
        removeDocument(Fields.ID.toString(), uuid)
    }

    fun updatePlaylist(uuid: String, updateModel: UpdatePlaylistModel) {
        val playlist = findPlaylist(uuid) ?: throw NullPointerException("There is no playlist with the uuid: $uuid")
        val playlistJson = JSONObject(playlist.toJson())

        if (updateModel.name != null)
            playlistJson.put(Fields.TITLE, updateModel.name)
        if (updateModel.description != null)
            playlistJson.put(Fields.DESCRIPTION, updateModel.description)
        if (updateModel.artwork_url != null)
            playlistJson.put(Fields.ARTWORK_URL, updateModel.artwork_url)

        upsertDocument(
            Document.parse(getDocument(Fields.ID.toString(), uuid)!!),
            Document.parse(playlistJson.toString())
        )
    }

    fun addTrackToPlaylist(uuid: String, track: PlaylistTrack) {
        val playlist = findPlaylist(uuid) ?: throw NullPointerException("There is no playlist with the uuid: $uuid")
        val playlistJson = JSONObject(playlist.toJson())
        playlistJson.getJSONArray(Fields.TRACKS.toString())
            .put(JSONObject(track.toJson()))

        upsertDocument(
            Document.parse(getDocument(Fields.ID.toString(), uuid)!!),
            Document.parse(playlistJson.toString())
        )
    }

    fun addTracksToPlaylist(uuid: String, tracks: List<PlaylistTrack>) {
        val playlist = findPlaylist(uuid) ?: throw NullPointerException("There is no playlist with the uuid: $uuid")
        val playlistJson = JSONObject(playlist.toJson())
        val arr = playlistJson.getJSONArray(Fields.TRACKS.toString())
        tracks.forEach { track -> arr.put(JSONObject(track.toJson())) }

        upsertDocument(
            Document.parse(getDocument(Fields.ID.toString(), uuid)!!),
            Document.parse(playlistJson.toString())
        )
    }

    fun removeTrackFromPlaylist(uuid: String, track: PlaylistTrack) {
        val playlist = findPlaylist(uuid) ?: throw NullPointerException("There is no playlist with the uuid: $uuid")
        val playlistJson = JSONObject(playlist.toJson())
        playlistJson.getJSONArray(Fields.TRACKS.toString())
            .remove(Fields.TRACK_ID, track.identifier)

        upsertDocument(
            Document.parse(getDocument(Fields.ID.toString(), uuid)!!),
            Document.parse(playlistJson.toString())
        )
    }

    enum class Fields(val str: String) : GenericJSONField {
        ID("id"),
        OWNER("owner"),
        ARTWORK_URL("artwork_url"),
        TITLE("title"),
        DESCRIPTION("description"),
        TRACKS("tracks"),
        TRACK_ID("identifier"),
        TRACK_TITLE("title"),
        TRACK_AUTHOR("author"),
        TRACK_DURATION("duration"),
        TRACK_ISRC("isrc"),
        TRACK_DATE_ADDED("date_added");

        override fun toString(): String = str
    }

}