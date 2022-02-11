package main.utils.database.postgresql.tracks;

import main.constants.ENV;
import main.main.Config;
import main.utils.database.postgresql.AbstractPostgresDB;
import main.utils.database.postgresql.AbstractPostgresTable;

import java.util.HashMap;

public class TrackDB extends AbstractPostgresDB {
    private final static String DATABASE_NAME = "tracks";
    private static TrackDB INSTANCE;

    protected TrackDB() {
        super(
                Config.get(ENV.POSTGRES_USERNAME),
                Config.get(ENV.POSTGRES_PASSWORD),
                Config.get(ENV.POSTGRES_HOST),
                Config.get(ENV.POSTGRES_PORT),
                DATABASE_NAME
        );
    }

    public SpotifyTracksTable getSpotifyTable() {
        return getTable(SpotifyTracksTable.TABLE_NAME, SpotifyTracksTable.class);
    }

    @Override
    public void initTables() {

    }

    @Override
    public void updateTables() {

    }

    @Override
    public HashMap<String, AbstractPostgresTable> getTables() {
        HashMap<String, AbstractPostgresTable> tables = new HashMap<>();
        tables.put(SpotifyTracksTable.TABLE_NAME, new SpotifyTracksTable(getInstance()));
        return tables;
    }

    public static TrackDB getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TrackDB();
        return INSTANCE;
    }
}
