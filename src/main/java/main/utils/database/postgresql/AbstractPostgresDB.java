package main.utils.database.postgresql;

import lombok.Getter;
import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public abstract class AbstractPostgresDB {
    @Getter
    private final Connection connection;

    @SneakyThrows
    protected AbstractPostgresDB(String user, String password, String host, @Nullable String port, String databaseName) {
        String url = "jdbc:postgresql://" + host + (port == null ? "" : ":" + port) + "/" + databaseName;
        this.connection = DriverManager.getConnection(url, user, password);
    }

    @SneakyThrows
    protected AbstractPostgresDB(String databaseName) {
        String url = "jdbc:postgresql://" + Config.get(ENV.POSTGRES_HOST) +
                (Config.get(ENV.POSTGRES_PORT) == null ? "" : ":"+ Config.get(ENV.POSTGRES_PORT)) +
                "/" + databaseName;
        this.connection = DriverManager.getConnection(url, Config.get(ENV.POSTGRES_USERNAME), Config.get(ENV.POSTGRES_PASSWORD));
    }

    public void initTables() throws SQLException {
        for (var table : getTables().values())
            initTable(table);
    }

    public void initTable(AbstractPostgresTable table) throws SQLException {
        if (!table.tableExists())
            table.init();
    }

    public abstract void updateTables();
    public abstract HashMap<String, AbstractPostgresTable> getTables();

    public <T extends AbstractPostgresTable> T getTable(String tableName, Class<T> clazz) {
        return (T) getTables().get(tableName);
    }
}
