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
    protected AbstractPostgresDB(String user, String password, String host, @Nullable String port, @Nullable String db) {
        String url = "jdbc:postgresql://" + host + (port == null ? "/" : ":" + port + "/") + (db == null ? "" : db);
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("ssl", "true");
        this.connection = DriverManager.getConnection(url, properties);
    }

    public abstract void initTables();
    public abstract void updateTables();
    public abstract HashMap<String, AbstractPostgresTable> getTables();

    public <T extends AbstractPostgresTable> T getTable(String tableName, Class<T> clazz) {
        return (T) getTables().get(tableName);
    }
}
