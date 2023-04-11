package main.utils.database.mongodb.databases

import main.constants.database.MongoDatabaseKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.json.GenericJSONFieldKt

class FavouriteTracksDBKt private constructor() : AbstractMongoDatabaseKt(MongoDatabaseKt.ROBERTIFY_DATABASE, MongoDatabaseKt.ROBERTIFY_FAVOURITE_TRACKS) {

    companion object {
        private var INSTANCE: FavouriteTracksDBKt? = null

        fun ins(): FavouriteTracksDBKt {
            if (INSTANCE == null)
                INSTANCE = FavouriteTracksDBKt()
            return INSTANCE!!
        }
    }

    enum class Field(private val str: String) : GenericJSONFieldKt {
        USER_ID("user_id"),
        TRACKS_ARRAY("tracks"),
        TRACK_ID("track_id"),
        TRACK_TITLE("track_title"),
        TRACK_AUTHOR("track_author"),
        TRACK_SOURCE("track_source");

        override fun toString(): String {
            return str
        }
    }

}