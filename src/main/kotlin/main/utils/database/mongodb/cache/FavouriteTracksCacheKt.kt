package main.utils.database.mongodb.cache

import main.constants.TrackSourceKt
import main.utils.database.mongodb.DocumentBuilderKt
import main.utils.database.mongodb.databases.FavouriteTracksDBKt
import org.bson.Document
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class FavouriteTracksCacheKt private constructor() : AbstractMongoCacheKt(FavouriteTracksDBKt) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        val instance: FavouriteTracksCacheKt by lazy { FavouriteTracksCacheKt() }
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
    fun addTrack(uid: Long, trackID: String, title: String, author: String, source: TrackSourceKt) {
        if (!userHasInfo(uid)) addUser(uid)
        require(!trackIsAdded(uid, trackID)) { "This track has already been added as a favourite track!" }
        val userInfo = getUserInfo(uid)
        val jsonArray = userInfo!!.getJSONArray(FavouriteTracksDBKt.Field.TRACKS_ARRAY.toString())
        jsonArray.put(
            JSONObject()
                .put(FavouriteTracksDBKt.Field.TRACK_ID.toString(), trackID)
                .put(FavouriteTracksDBKt.Field.TRACK_TITLE.toString(), title)
                .put(FavouriteTracksDBKt.Field.TRACK_AUTHOR.toString(), author)
                .put(FavouriteTracksDBKt.Field.TRACK_SOURCE.toString(), source.name.lowercase(Locale.getDefault()))
        )
        updateCache(userInfo, FavouriteTracksDBKt.Field.USER_ID, uid)
    }

    @Synchronized
    fun removeTrack(uid: Long, index: Int) {
        if (!userHasInfo(uid)) throw java.lang.NullPointerException("This user doesn't have any information")
        val userInfo = getUserInfo(uid)
        val jsonArray = userInfo!!.getJSONArray(FavouriteTracksDBKt.Field.TRACKS_ARRAY.toString())
        jsonArray.remove(index)
        updateCache(userInfo, FavouriteTracksDBKt.Field.USER_ID, uid)
    }

    @Synchronized
    fun clearTracks(uid: Long) {
        if (!userHasInfo(uid)) throw java.lang.NullPointerException("This user doesn't have any information")
        val userInfo = getUserInfo(uid)
        val jsonArray = userInfo!!.getJSONArray(FavouriteTracksDBKt.Field.TRACKS_ARRAY.toString())
        jsonArray.clear()
        updateCache(userInfo, FavouriteTracksDBKt.Field.USER_ID, uid)
    }

    @Synchronized
    fun getTracks(uid: Long): List<Track> {
        if (!userHasInfo(uid)) addUser(uid)
        val ret: MutableList<Track> = ArrayList()
        val arr = getUserInfo(uid)!!.getJSONArray(FavouriteTracksDBKt.Field.TRACKS_ARRAY.toString())
        for (obj in arr) {
            val actualObj = obj as JSONObject
            ret.add(
                Track(
                    actualObj.getString(FavouriteTracksDBKt.Field.TRACK_ID.toString()),
                    actualObj.getString(FavouriteTracksDBKt.Field.TRACK_TITLE.toString()),
                    actualObj.getString(FavouriteTracksDBKt.Field.TRACK_AUTHOR.toString()),
                    TrackSourceKt.parse(actualObj.getString(FavouriteTracksDBKt.Field.TRACK_SOURCE.toString()))
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
                FavouriteTracksDBKt.Field.USER_ID,
                userId
            )
        )
    }

    fun userHasInfo(uid: Long): Boolean = try {
        getCache().getJSONObject(getIndexOfObjectInArray(getCache(), FavouriteTracksDBKt.Field.USER_ID, uid))
        true
    } catch (e: JSONException) {
        false
    } catch (e: NullPointerException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    private fun getDefaultDocument(userID: Long): Document {
        return DocumentBuilderKt.create()
            .addField(FavouriteTracksDBKt.Field.USER_ID, userID)
            .addField(FavouriteTracksDBKt.Field.TRACKS_ARRAY, JSONArray())
            .build()
    }

    data class Track(val id: String, val title: String, val author: String, val source: TrackSourceKt) {}

}