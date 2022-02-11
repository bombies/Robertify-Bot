package main.utils.database.postgresql.tracks;

import lombok.Getter;
import main.utils.database.postgresql.AbstractPostgresTable;

public class SpotifyTracksTable extends AbstractPostgresTable {
    @Getter
    private final TrackDB database;
    public final static String TABLE_NAME = "spotifytracks";

    protected SpotifyTracksTable(TrackDB db) {
        super(db.getConnection());
        this.database = db;
    }

    public void addTrack(String spotifyID, String youTubeID) {
        // TODO
    }

    public String getTrackYouTubeID(String spotifyID, String youTubeID) {
        // TODO
        return "";
    }

    public void removeTrack(String spotifyID) {
        // TODO
    }

    public boolean trackExists(String spotifyID) {
        // TODO
        return false;
    }
}
