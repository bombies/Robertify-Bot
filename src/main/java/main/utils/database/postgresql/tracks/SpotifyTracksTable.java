package main.utils.database.postgresql.tracks;

import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.database.postgresql.AbstractPostgresTable;

import java.sql.Connection;
import java.sql.ResultSet;

public class SpotifyTracksTable extends AbstractPostgresTable {
    @Getter
    private final TrackDB database;
    private final Connection con;
    public final static String TABLE_NAME = "spotifytracks";
    private final static int SPOTIFY_ID_LENGTH = 22;
    private final static int YOUTUBE_ID_LENGTH = 11;

    protected SpotifyTracksTable(TrackDB db) {
        super(db.getConnection());
        this.database = db;
        this.con = db.getConnection();
    }

    @SneakyThrows
    public void addTrack(String spotifyID, String youTubeID) {
        if (trackExists(spotifyID))
            removeTrack(spotifyID);

        final String sql = "INSERT INTO " + TABLE_NAME + " VALUES('"+spotifyID+"', '"+youTubeID+"');";
        con.createStatement().executeUpdate(sql);
    }

    @SneakyThrows
    public String getTrackYouTubeID(String spotifyID, String youTubeID) {
        if (!trackExists(spotifyID))
            return null;

        final String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + Fields.SPOTIFY_ID + "='"+spotifyID+"';";
        ResultSet resultSet = con.createStatement().executeQuery(sql);

        while (resultSet.next())
            return resultSet.getString(Fields.YOUTUBE_ID.toString());
        throw new NullPointerException("There was no matching YouTube track ID found for Spotify track with ID: " + spotifyID);
    }

    @SneakyThrows
    public void removeTrack(String spotifyID) {
        final String sql = "DELETE FROM " + TABLE_NAME + " WHERE " + Fields.SPOTIFY_ID + "='"+spotifyID+"';";
        con.createStatement().executeUpdate(sql);
    }

    @SneakyThrows
    public boolean trackExists(String spotifyID) {
        final String sql= "SELECT * FROM " + TABLE_NAME + " WHERE " + Fields.SPOTIFY_ID + "='"+spotifyID+"';";
        ResultSet resultSet = con.createStatement().executeQuery(sql);
        return resultSet.next();
    }

    @Override @SneakyThrows
    public void init() {
        final String sql = "CREATE TABLE " + TABLE_NAME + "(" +
                    ""+Fields.SPOTIFY_ID+" char("+SPOTIFY_ID_LENGTH+") PRIMARY KEY," +
                    ""+Fields.YOUTUBE_ID+" char("+YOUTUBE_ID_LENGTH+")" +
                ");";
        con.createStatement().execute(sql);
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
