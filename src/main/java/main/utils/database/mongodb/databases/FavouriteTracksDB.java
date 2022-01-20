package main.utils.database.mongodb.databases;

import main.constants.Database;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.json.GenericJSONField;

public class FavouriteTracksDB extends AbstractMongoDatabase {
    private static FavouriteTracksDB INSTANCE;

    private FavouriteTracksDB() {
        super(Database.MONGO.ROBERTIFY_DATABASE, Database.MONGO.ROBERTIFY_FAVOURITE_TRACKS);
    }

    @Override
    public void init() {
        // Nothing to init
    }

    public static FavouriteTracksDB ins() {
        if (INSTANCE == null)
            INSTANCE = new FavouriteTracksDB();
        return INSTANCE;
    }

    public enum Field implements GenericJSONField {
        USER_ID("user_id"),
        TRACKS_ARRAY("tracks"),
        TRACK_ID("track_id"),
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
