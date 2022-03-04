package main.utils.database.postgresql.tracks;

import lombok.SneakyThrows;
import main.utils.database.postgresql.AbstractPostgresTable;

import java.sql.Connection;
import java.sql.ResultSet;

public class TrackTable extends AbstractPostgresTable {
    private final TrackDB database;
    private final Connection con;
    private final Table table;
    private final static int SPOTIFY_ID_LENGTH = 22;
    private final static int DEEZER_ID_LENGTH = 10;
    private final static int YOUTUBE_ID_LENGTH = 11;

    protected TrackTable(TrackDB database, Table table) {
        super(database.getConnection(), table.toString());
        this.database = database;
        this.con = database.getConnection();
        this.table = table;
    }

    @SneakyThrows
    public void addTrack(String sourceID, String youTubeID) {
        if (trackExists(sourceID))
            removeTrack(sourceID);

        final String sql = "INSERT INTO " + getTableName() + " VALUES('"+sourceID+"', '"+youTubeID+"');";
        con.createStatement().executeUpdate(sql);
    }

    @SneakyThrows
    public String getTrackYouTubeID(String sourceID) {
        if (!trackExists(sourceID))
            return null;

        final String sql = "SELECT * FROM " + getTableName() + " WHERE " + (table.equals(Table.SPOTIFY) ?
                SpotifyTracksTable.Fields.SPOTIFY_ID : DeezerTracksTable.Fields.DEEZER_ID) + "='"+sourceID+"';";

        String ret = getString(sql);

        if (ret == null)
            throw new NullPointerException("There was no matching YouTube track ID found for Spotify track with ID: " + sourceID);
        return ret;
    }

    @SneakyThrows
    public void removeTrack(String sourceID) {
        final String sql = "DELETE FROM " + getTableName() + " WHERE " + (table.equals(Table.SPOTIFY) ?
                SpotifyTracksTable.Fields.SPOTIFY_ID : DeezerTracksTable.Fields.DEEZER_ID) + "='"+sourceID+"';";
        con.createStatement().executeUpdate(sql);
    }

    @SneakyThrows
    public boolean trackExists(String sourceID) {
        final String sql= "SELECT * FROM " + getTableName() + " WHERE " + (table.equals(Table.SPOTIFY) ?
                SpotifyTracksTable.Fields.SPOTIFY_ID : DeezerTracksTable.Fields.DEEZER_ID) + "='"+sourceID+"';";
        ResultSet resultSet = con.createStatement().executeQuery(sql);
        return resultSet.next();
    }

    @Override @SneakyThrows
    public void init() {
        switch (table) {
            case DEEZER: {
                final String sql = "CREATE TABLE " + getTableName() + "(" +
                        ""+ DeezerTracksTable.Fields.DEEZER_ID+" char("+DEEZER_ID_LENGTH+") PRIMARY KEY," +
                        ""+ DeezerTracksTable.Fields.YOUTUBE_ID+" char("+YOUTUBE_ID_LENGTH+")" +
                        ");";
                con.createStatement().execute(sql);
            }
            case SPOTIFY: {
                final String sql = "CREATE TABLE " + getTableName() + "(" +
                        ""+ SpotifyTracksTable.Fields.SPOTIFY_ID+" char("+SPOTIFY_ID_LENGTH+") PRIMARY KEY," +
                        ""+ SpotifyTracksTable.Fields.YOUTUBE_ID+" char("+YOUTUBE_ID_LENGTH+")" +
                        ");";
                con.createStatement().execute(sql);
            }
        }
    }

    public String getTableName() {
        return table.toString();
    }

    public enum Table {
        SPOTIFY("spotifytracks"),
        DEEZER("deezertracks");

        private final String str;

        Table(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
