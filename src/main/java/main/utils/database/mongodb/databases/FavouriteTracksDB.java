package main.utils.database.mongodb.databases;

import main.constants.Database;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.json.GenericJSONField;

public class FavouriteTracksDB extends AbstractMongoDatabase {
    private static FavouriteTracksDB INSTANCE;

    private FavouriteTracksDB() {
        super(Database.Mongo.ROBERTIFY_DATABASE, Database.Mongo.ROBERTIFY_FAVOURITE_TRACKS);
    }

    @Override
    public void init() {
        // Nothing to init
    }

    public synchronized static FavouriteTracksDB ins() {
        if (INSTANCE == null)
            INSTANCE = new FavouriteTracksDB();
        return INSTANCE;
    }

    public enum Field implements GenericJSONField {
        USER_ID("user_id"),
        TRACKS_ARRAY("tracks"),
        TRACK_ID("track_id"),
        TRACK_TITLE("track_title"),
        TRACK_AUTHOR("track_author"),
        TRACK_SOURCE("track_source");

        private final String str;

        Field(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
