package main.utils.database.postgresql.tracks;

public class DeezerTracksTable extends TrackTable {
    public static String TABLE_NAME = Table.DEEZER.toString();

    protected DeezerTracksTable(TrackDB database) {
        super(database, Table.DEEZER);
    }

    public enum Fields {
        YOUTUBE_ID,
        DEEZER_ID;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
