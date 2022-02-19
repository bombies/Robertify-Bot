package main.utils.database.postgresql.tracks;

public class SpotifyTracksTable extends TrackTable {
    public static String TABLE_NAME = Table.SPOTIFY.toString();

    protected SpotifyTracksTable(TrackDB db) {
        super(db, Table.SPOTIFY);
    }

    public enum Fields {
        YOUTUBE_ID,
        SPOTIFY_ID;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
