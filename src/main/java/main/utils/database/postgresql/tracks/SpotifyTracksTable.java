package main.utils.database.postgresql.tracks;

import lombok.Getter;

public class SpotifyTracksTable extends TrackTable {
    public static String TABLE_NAME = TrackTable.Table.SPOTIFY.toString();

    protected SpotifyTracksTable(TrackDB db) {
        super(db, TrackTable.Table.SPOTIFY);
    }

    public enum Fields {
        TRACK_ID("track_id"),
        ARTIST_ID("artist_id"),
        GENRE("genre");

        @Getter
        private final String str;

        Fields(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return this.str;
        }
    }
}
