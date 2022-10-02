package main.utils.database.postgresql.tracks;

import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.database.postgresql.AbstractPostgresTable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;

public class TrackTable extends AbstractPostgresTable {
    private final TrackDB database;

    private final Connection con;

    private final Table table;

    protected TrackTable(TrackDB database, Table table) {
        super(database.getConnection(), table.toString());
        this.database = database;
        this.con = database.getConnection();
        this.table = table;
    }

    @SneakyThrows
    public void addTrack(String trackID, String artistID, String genre) {
        if (trackExists(trackID))
            return;
        String sql = "INSERT INTO " + getTableName() + " VALUES('" + trackID + "', '" + artistID + "', '"+genre+"');";
        this.con.createStatement().executeUpdate(sql);
    }

    @SneakyThrows
    public void addTrack(String trackID, String artistID, String[] genre) {
        addTrack(trackID, artistID, Arrays.toString(genre).replaceAll("[\\[\\]\\s]", ""));
    }

    public void addTrack(String trackID, String artistID, Collection<String> genre) {
        addTrack(trackID, artistID, genre.toString().replaceAll("[\\[\\]\\s]", ""));
    }

    @SneakyThrows
    public TrackInfo getTrackInfo(String trackID) {
        if (!trackExists(trackID))
            return null;
        final var sql = "SELECT * FROM " + getTableName() + " WHERE " + SpotifyTracksTable.Fields.TRACK_ID +"='"+trackID+"';";
        final var res = con.prepareStatement(sql).executeQuery();
        if (res.next())
            return new TrackInfo(
                    res.getString(SpotifyTracksTable.Fields.TRACK_ID.toString()),
                    res.getString(SpotifyTracksTable.Fields.ARTIST_ID.toString()),
                    res.getString(SpotifyTracksTable.Fields.GENRE.toString())
            );
        throw new NullPointerException("There was no Spotify track with ID "+trackID+" found in the database!");
    }

    @SneakyThrows
    public void removeTrack(String sourceID) {
        String sql = "DELETE FROM " + getTableName() + " WHERE " + (this.table.equals(Table.SPOTIFY) ? SpotifyTracksTable.Fields.TRACK_ID : DeezerTracksTable.Fields.DEEZER_ID) + "='" + sourceID + "';";
        this.con.createStatement().executeUpdate(sql);
    }

    @SneakyThrows
    public boolean trackExists(String sourceID) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + (this.table.equals(Table.SPOTIFY) ? SpotifyTracksTable.Fields.TRACK_ID : DeezerTracksTable.Fields.DEEZER_ID) + "='" + sourceID + "';";
        ResultSet resultSet = this.con.createStatement().executeQuery(sql);
        return resultSet.next();
    }

    @SneakyThrows
    public void init() {
        String sql;
        switch (this.table) {
            case SPOTIFY:
                sql = "CREATE TABLE " + getTableName() + "(" + SpotifyTracksTable.Fields.TRACK_ID + " char(22) PRIMARY KEY," + SpotifyTracksTable.Fields.ARTIST_ID + " char(22) NOT NULL, "+ SpotifyTracksTable.Fields.GENRE +" char(64) NOT NULL);";
                this.con.createStatement().execute(sql);
                break;
        }
    }

    public String getTableName() {
        return this.table.toString();
    }

    public static class TrackInfo {
        @Getter
        final String trackID, artistID, genre;

        public TrackInfo(String trackID, String artistID, String genre) {
            this.trackID = trackID;
            this.artistID = artistID;
            this.genre = genre;
        }
    }

    public enum Table {
        SPOTIFY("spotifytracks_new"),
        DEEZER("deezertracks_new");

        private final String str;

        Table(String str) {
            this.str = str;
        }

        public String toString() {
            return this.str;
        }
    }
}
