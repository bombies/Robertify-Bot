package main.utils.database.sqlite3;

import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.DatabaseTable;
import main.constants.ENV;
import main.main.Config;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ServerDB extends AbstractSQLiteDatabase {
    private static final Map<@NotNull Long, @NotNull String> prefixes = new HashMap<>();

    public ServerDB() {
        super(Database.MAIN);
    }

    /**
     * Set the prefix for the bot in a specific server
     * @param gid ID of the guild
     * @param prefix Prefix to be set
     */
    @SneakyThrows
    public ServerDB setServerPrefix(long gid, @NotNull String prefix) {
        Statement dbStat = getCon().createStatement();
        String sql = "INSERT INTO " + DatabaseTable.MAIN_BOT_INFO + "(server_id, prefix) " +
                "VALUES("+gid+", '"+prefix+"');";
        dbStat.executeUpdate(sql);
        return this;
    }

    /**
     * Updates the prefix of the bot in a specific server
     * @param gid ID of the guild
     * @param prefix Prefix to be set
     */
    @SneakyThrows
    public ServerDB updateServerPrefix(long gid, @NotNull String prefix) {
        Statement dbStat = getCon().createStatement();
        String sql = "UPDATE " + DatabaseTable.MAIN_BOT_INFO + " SET prefix='"+prefix+"' WHERE server_id="+gid+";";
        dbStat.executeUpdate(sql);

        prefixes.put(gid, prefix);

        return this;
    }

    /**
     * Get the current prefix for a guild
     * @param gid ID of the guild
     * @return The prefix the guild currently has assigned to the bot
     */
    @SneakyThrows
    public String getServerPrefix(long gid) {
        Statement dbStat = getCon().createStatement();
        String sql = "SELECT prefix FROM " + DatabaseTable.MAIN_BOT_INFO + " WHERE server_id="+gid+";";
        ResultSet dbRest = dbStat.executeQuery(sql);

        while (dbRest.next()) {
            String ret = dbRest.getString("prefix");
            getCon().close();
            return ret;
        }

        throw new NullPointerException("There was nothing found for guild with ID \""+gid+"\"");
    }

    @SneakyThrows
    public static void initPrefixMap() {
        for (Guild g : new BotDB().getGuilds()) {
            String serverPrefix = new ServerDB().getServerPrefix(g.getIdLong());

            if (serverPrefix != null)
                prefixes.put(g.getIdLong(), serverPrefix);
            else
                prefixes.put(g.getIdLong(), Config.get(ENV.PREFIX));
        }
    }

    public static String getPrefix(long gid) {
        return prefixes.get(gid);
    }
}
