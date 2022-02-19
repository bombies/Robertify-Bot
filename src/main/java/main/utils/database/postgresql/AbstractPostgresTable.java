package main.utils.database.postgresql;

import lombok.Getter;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPostgresTable {
    private final Connection connection;
    @Getter
    private final String tableName;

    protected AbstractPostgresTable(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    @SneakyThrows
    public <T> T getObject(String sql, Class<T> clazz, int column) {
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        while (resultSet.next())
            return resultSet.getObject(column, clazz);
        return null;
    }

    @SneakyThrows
    public boolean tableExists() {
        ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null);
        return tables.next();
    }

    @SneakyThrows
    public <T> List<T> getSimpleList(String sql, Class<T> clazz, int column) {
        final List<T> ret = new ArrayList<>();

        ResultSet resultSet = connection.createStatement().executeQuery(sql);

        while (resultSet.next())
            ret.add(resultSet.getObject(column, clazz));

        resultSet.close();

        return ret;
    }

    public abstract void init();
}
