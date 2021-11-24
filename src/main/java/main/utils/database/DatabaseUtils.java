package main.utils.database;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.DatabaseTable;
import main.constants.ENV;
import main.main.Config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class DatabaseUtils {
    @Getter
    private Connection con = null;
    @Setter @Getter
    private static Database database = null;

    @SneakyThrows
    public DatabaseUtils(Database db) {
        String url = "jdbc:sqlite:" + Config.get(ENV.DATABASE_DIR) + "/" + db.toString() + ".db";

        boolean dbExists = databaseExists(db);

        if (!dbExists)
            new File(Config.get(ENV.DATABASE_DIR) + "/" + db + ".db").createNewFile();

        con = DriverManager.getConnection(url);

        if (!dbExists)
            createTables(db);

        setDatabase(db);
    }

    /**
     * Creates all the tables necessary for the passed database
     * @param db The database to create the tables for.
     */
    @SneakyThrows
    private void createTables(Database db) {
        switch (db) {
            case MAIN -> {
                Statement dbStat = con.createStatement();
                String sql = "CREATE TABLE " + DatabaseTable.MAIN_BOT_INFO + " (" +
                        "server_id INTEGER PRIMARY KEY," +
                        "prefix TEXT NOT NULL," +
                        "announce_msgs BOOLEAN," +
                        "announcement_channel INTEGER" +
                        ");";

                String sql2 = "CREATE TABLE " + DatabaseTable.MAIN_BOT_DEVELOPERS + " (" +
                        "developer_id INTEGER" +
                        ");";

                dbStat.execute(sql);
                dbStat.execute(sql2);
            }
            case BANNED_USERS -> {
                Statement dbStat = con.createStatement();
                String sql = "CREATE TABLE " + DatabaseTable.BANNED_USERS_TABLE + " (" +
                        "guild_id INTEGER," +
                        "banned_id INTEGER," +
                        "banned_by INTEGER," +
                        "banned_at INTEGER," +
                        "banned_until INTEGER"
                        + ");";

                dbStat.execute(sql);
            }
        }
    }

    /**
     * Checks if a specific database exists in the directory
     * @param db Database to be checked
     * @return True if the database exists, false if it doesnt.
     */
    @SneakyThrows
    private boolean databaseExists(Database db) {
        if (!Files.exists(Path.of(Config.get(ENV.DATABASE_DIR) + "/")))
            Files.createDirectories(Paths.get(Config.get(ENV.DATABASE_DIR)));

        return Files.exists(Path.of(Config.get(ENV.DATABASE_DIR) + "/" + db.toString() + ".db"));
    }

    /**
     * Checks if a table exists in the main database
     *
     * @param table The enum of the table name.
     * @return Will return true if the table does exist in the database and false if it doesn't.
     * @throws SQLException Thrown when any SQL error is encountered.
     */
    public boolean tableExists(DatabaseTable table) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        ResultSet dbRes = meta.getTables(null, null, table.toString(), new String[] {"TABLE"});
        return dbRes.next();
    }

    /**
     * Execute SQL commands such as `INSERT`, `UPDATE`, `ALTER`, `DELETE` and etc.
     *
     * @param sql The string of the SQL query that is to be executed.
     * @throws SQLException Thrown when any SQL error is encountered.
     */
    public void executeAnyUpdate(String sql) throws SQLException {
        Statement dbStat = con.createStatement();
        dbStat.executeUpdate(sql);
    }

    /**
     * Execute any SQL commands that aren't related to table updating such as `CREATE TABLE, `DROP TABLE` etc.
     *
     * @param sql The string of the SQL query that is to be executed.
     * @throws SQLException Thrown when any SQL error is encountered.
     */
    public void executeAnyBase(String sql) throws SQLException {
        Statement dbStat = con.createStatement();
        dbStat.execute(sql);
    }

    /**
     * Closes the database connection
     */
    @SneakyThrows
    public void closeConnection() {
        con.close();
    }

    @SneakyThrows
    public void createConnection(Database db) {
        String url = "jdbc:sqlite:" + Config.get(ENV.DATABASE_DIR) + "/" + db.toString() + ".db";
        con = DriverManager.getConnection(url);
    }

    @SneakyThrows
    public void createConnection() {
        if (database == null)
            throw new NullPointerException("There is no database!");

        String url = "jdbc:sqlite:" + Config.get(ENV.DATABASE_DIR) + "/" + database.toString() + ".db";
        con = DriverManager.getConnection(url);
    }

    @SneakyThrows
    public void openConnectionIfClosed() {
        if (con.isClosed())
            createConnection();
    }
}
