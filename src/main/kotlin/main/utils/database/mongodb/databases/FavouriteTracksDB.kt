package main.utils.database.mongodb.databases

import main.constants.database.RobertifyMongoDatabase
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.json.GenericJSONField

object FavouriteTracksDB : AbstractMongoDatabase(RobertifyMongoDatabase.ROBERTIFY_DATABASE, RobertifyMongoDatabase.ROBERTIFY_FAVOURITE_TRACKS) {

    enum class Field(private val str: String) : GenericJSONField {
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