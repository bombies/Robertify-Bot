package main.utils.database.mongodb.cache

import main.constants.TrackSource
import main.utils.database.mongodb.DocumentBuilder
import main.utils.database.mongodb.databases.FavouriteTracksDB
import org.bson.Document
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class FavouriteTracksCache private constructor() : AbstractMongoCache(FavouriteTracksDB) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        val instance: FavouriteTracksCache by lazy { FavouriteTracksCache() }
    }

    init {
        updateCache()
        logger.debug("Done instantiating Favourite Tracks cache")
    }

    @Synchronized
    fun addUser(uid: Long) {
        require(!userHasInfo(uid)) { "This user already has information!" }
        addToCache(getDefaultDocument(uid))
    }

    @Synchronized
    fun addTrack(uid: Long, trackID: String, title: String, author: String, source: TrackSource) {
        if (!userHasInfo(uid)) addUser(uid)
        require(!trackIsAdded(uid, trackID)) { "This track has already been added as a favourite track!" }
        val userInfo = getUserInfo(uid)
        val jsonArray = userInfo!!.getJSONArray(FavouriteTracksDB.Field.TRACKS_ARRAY.toString())
        jsonArray.put(
            JSONObject()
                .put(FavouriteTracksDB.Field.TRACK_ID.toString(), trackID)
                .put(FavouriteTracksDB.Field.TRACK_TITLE.toString(), title)
                .put(FavouriteTracksDB.Field.TRACK_AUTHOR.toString(), author)
                .put(FavouriteTracksDB.Field.TRACK_SOURCE.toString(), source.name.lowercase(Locale.getDefault()))
        )
        updateCache(userInfo, FavouriteTracksDB.Field.USER_ID, uid)
    }

    @Synchronized
    fun removeTrack(uid: Long, index: Int) {
        if (!userHasInfo(uid)) throw java.lang.NullPointerException("This user doesn't have any information")
        val userInfo = getUserInfo(uid)
        val jsonArray = userInfo!!.getJSONArray(FavouriteTracksDB.Field.TRACKS_ARRAY.toString())
        jsonArray.remove(index)
        updateCache(userInfo, FavouriteTracksDB.Field.USER_ID, uid)
    }

    @Synchronized
    fun clearTracks(uid: Long) {
        if (!userHasInfo(uid)) throw java.lang.NullPointerException("This user doesn't have any information")
        val userInfo = getUserInfo(uid)
        val jsonArray = userInfo!!.getJSONArray(FavouriteTracksDB.Field.TRACKS_ARRAY.toString())
        jsonArray.clear()
        updateCache(userInfo, FavouriteTracksDB.Field.USER_ID, uid)
    }

    @Synchronized
    fun getTracks(uid: Long): List<Track> {
        if (!userHasInfo(uid)) addUser(uid)
        val ret: MutableList<Track> = ArrayList()
        val arr = getUserInfo(uid)!!.getJSONArray(FavouriteTracksDB.Field.TRACKS_ARRAY.toString())
        for (obj in arr) {
            val actualObj = obj as JSONObject
            ret.add(
                Track(
                    actualObj.getString(FavouriteTracksDB.Field.TRACK_ID.toString()),
                    actualObj.getString(FavouriteTracksDB.Field.TRACK_TITLE.toString()),
                    actualObj.getString(FavouriteTracksDB.Field.TRACK_AUTHOR.toString()),
                    TrackSource.parse(actualObj.getString(FavouriteTracksDB.Field.TRACK_SOURCE.toString()))
                )
            )
        }
        return ret
    }

    @Synchronized
    fun trackIsAdded(uid: Long, trackID: String): Boolean {
        if (!userHasInfo(uid)) {
            addUser(uid)
            return false
        }
        if (getTracks(uid).isEmpty()) return false
        for (track in getTracks(uid)) if (track.id == trackID) return true
        return false
    }

    @Synchronized
    fun getUserInfo(userId: Long): JSONObject? {
        val cache = getCache()

        return if (!userHasInfo(userId)) null else cache.getJSONObject(
            getIndexOfObjectInArray(
                cache,
                FavouriteTracksDB.Field.USER_ID,
                userId
            )
        )
    }

    fun userHasInfo(uid: Long): Boolean = try {
        getCache().getJSONObject(getIndexOfObjectInArray(getCache(), FavouriteTracksDB.Field.USER_ID, uid))
        true
    } catch (e: JSONException) {
        false
    } catch (e: NullPointerException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    private fun getDefaultDocument(userID: Long): Document {
        return DocumentBuilder.create()
            .addField(FavouriteTracksDB.Field.USER_ID, userID)
            .addField(FavouriteTracksDB.Field.TRACKS_ARRAY, JSONArray())
            .build()
    }

    data class Track(val id: String, val title: String, val author: String, val source: TrackSource) {}

}