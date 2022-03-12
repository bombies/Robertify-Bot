package main.utils.database.postgresql;

import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractPostgresTable {
    @Getter
    private final Connection connection;
    @Getter
    private final String tableName;

    protected AbstractPostgresTable(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    public ResultSet getResultSet(String sql) throws SQLException {
        return connection.createStatement().executeQuery(sql);
    }

    public String getString(String sql) {
        return getObject(sql, String.class);
    }

    public Integer getInt(String sql) {
        return getObject(sql, Integer.class);
    }

    public Long getLong(String sql) {
        return getObject(sql, Long.class);
    }

    public Double getDouble(String sql) {
        return getObject(sql, Double.class);
    }

    public List<String> getStringList(String sql) throws SQLException {
        return getSimpleList(sql, String.class, 1);
    }

    public List<Integer> getIntegerList(String sql) throws SQLException {
        return getSimpleList(sql, Integer.class, 1);
    }

    public List<Double> getDoubleList(String sql) throws SQLException {
        return getSimpleList(sql, Double.class, 1);
    }

    @SneakyThrows
    private <T> T getObject(String sql, Class<T> clazz) {
        return getObject(sql, clazz, 1);
    }

    private <T> T getObject(String sql, Class<T> clazz, int column) throws SQLException {
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        while (resultSet.next())
            return resultSet.getObject(column, clazz);
        return null;
    }

    public boolean tableExists() throws SQLException {
        ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null);
        return tables.next();
    }

    private <T> List<T> getSimpleList(String sql, Class<T> clazz, int column) throws SQLException {
        final List<T> ret = new ArrayList<>();

        ResultSet resultSet = connection.createStatement().executeQuery(sql);

        while (resultSet.next())
            ret.add(resultSet.getObject(column, clazz));

        resultSet.close();

        return ret;
    }

    private <K, V> HashMap<K, V> getMap(String sql, Class<K> keyClazz, Class<V> valueClazz) {
        throw new UnsupportedOperationException("This hasn't been implemented yet!");
    }

    private <K, V> Pair<K,V> getPair(String sql, Class<K> keyClazz, Class<V> valueClazz) {
        throw new UnsupportedOperationException("This hasn't been implemented yet!");
    }

    public abstract void init();
}
