package main.utils.database.postgresql.voters;

import lombok.SneakyThrows;
import main.utils.database.postgresql.AbstractPostgresDB;
import main.utils.database.postgresql.AbstractPostgresTable;

import java.util.HashMap;

public class VoterDB extends AbstractPostgresDB {
    private final static String DATABASE_NAME = "voters";
    private static VoterDB INSTANCE;

    protected VoterDB() {
        super(DATABASE_NAME);
    }

    @SneakyThrows
    public VoterTable getVoterTable() {
        final var table = getTable(VoterTable.TABLE_NAME, VoterTable.class);
        initTable(table);
        return table;
    }

    @Override
    public void updateTables() {

    }

    @Override
    public HashMap<String, AbstractPostgresTable> getTables() {
        final HashMap<String, AbstractPostgresTable> tables = new HashMap<>();
        tables.put(VoterTable.TABLE_NAME, new VoterTable(getInstance()));
        return tables;
    }

    public static VoterDB getInstance() {
        if (INSTANCE == null)
            INSTANCE = new VoterDB();
        return INSTANCE;
    }
}
