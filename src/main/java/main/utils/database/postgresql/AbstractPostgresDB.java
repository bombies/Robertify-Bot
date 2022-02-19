package main.utils.database.postgresql;

import lombok.Getter;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Properties;

public abstract class AbstractPostgresDB {
    @Getter
    private final Connection connection;

    @SneakyThrows
    protected AbstractPostgresDB(String user, String password, String host, @Nullable String port, String databaseName) {
        String url = "jdbc:postgresql://" + host + (port == null ? "" : ":" + port) + "/" + databaseName;
        this.connection = DriverManager.getConnection(url, user, password);
    }

    public abstract void initTables();

    public void initTable(AbstractPostgresTable table){
        if (!table.tableExists())
            table.init();
    }

    public abstract void updateTables();
    public abstract HashMap<String, AbstractPostgresTable> getTables();

    public <T extends AbstractPostgresTable> T getTable(String tableName, Class<T> clazz) {
        return (T) getTables().get(tableName);
    }
}
