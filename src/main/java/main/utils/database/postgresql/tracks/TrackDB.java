package main.utils.database.postgresql.tracks;

import lombok.SneakyThrows;
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

    @SneakyThrows
    public SpotifyTracksTable getSpotifyTable() {
        SpotifyTracksTable table = getTable(SpotifyTracksTable.TABLE_NAME, SpotifyTracksTable.class);
        initTable(table);
        return table;
    }

    @SneakyThrows
    public DeezerTracksTable getDeezerTable() {
        DeezerTracksTable table = getTable(DeezerTracksTable.TABLE_NAME, DeezerTracksTable.class);
        initTable(table);
        return table;
    }

    @Override
    public void updateTables() {

    }

    @Override
    public HashMap<String, AbstractPostgresTable> getTables() {
        HashMap<String, AbstractPostgresTable> tables = new HashMap<>();
        tables.put(SpotifyTracksTable.TABLE_NAME, new SpotifyTracksTable(getInstance()));
        tables.put(DeezerTracksTable.TABLE_NAME, new DeezerTracksTable(getInstance()));
        return tables;
    }

    public static TrackDB getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TrackDB();
        return INSTANCE;
    }
}
