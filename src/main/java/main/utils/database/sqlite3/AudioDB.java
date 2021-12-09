package main.utils.database.sqlite3;

import lombok.Getter;
import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.DatabaseTable;

import java.util.HashMap;

public class AudioDB extends AbstractSQLiteDatabase {
    @Getter
    private static final HashMap<String, String> cachedTracks = new HashMap<>();

    public AudioDB() {
        super(Database.TRACKS_PLAYED);
    }

    @SneakyThrows
    public AudioDB addTrack(String spotifyID, String youTubeID) {
        if (isTrackCached(spotifyID))
            throw new IllegalArgumentException(spotifyID + " is already in the database!");

        openConnectionIfClosed();

        final var statement = getCon().createStatement();
        final var sql = "INSERT INTO " + DatabaseTable.SPOTIFY_TRACKS_TABLE + " VALUES(" +
                "'"+spotifyID+"', '"+youTubeID+"');";
        statement.executeUpdate(sql);

        cachedTracks.put(spotifyID, youTubeID);
        return this;
    }

    @SneakyThrows
    public void cacheAllTracks() {
        openConnectionIfClosed();

        final var statement = getCon().createStatement();
        final var sql = "SELECT * FROM " + DatabaseTable.SPOTIFY_TRACKS_TABLE + ";";
        final var resultSet = statement.executeQuery(sql);

        while (resultSet.next())
            cachedTracks.put(resultSet.getString("spotify_id"), resultSet.getString("youtube_id"));
    }

    public boolean isTrackCached(String spotifyID) {
        return cachedTracks.containsKey(spotifyID);
    }

    public String getYouTubeIDFromSpotify(String spotifyID) {
        return cachedTracks.get(spotifyID);
    }
}
