package main.utils.database.postgresql;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPostgresTable {
    private final Connection connection;

    protected AbstractPostgresTable(Connection connection) {
        this.connection = connection;
    }

    @SneakyThrows
    public <T> T getObject(String sql, Class<T> clazz, int column) {
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        while (resultSet.next())
            return resultSet.getObject(column, clazz);
        return null;
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
