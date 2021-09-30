package main.utils.database;

import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.DatabaseTable;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ServerUtils extends DatabaseUtils {
    private static Map<@NotNull Long, @NotNull String> prefixes = new HashMap<>();

    public ServerUtils() {
        super(Database.MAIN);
    }

    /**
     * Set the prefix for the bot in a specific server
     * @param gid ID of the guild
     * @param prefix Prefix to be set
     */
    @SneakyThrows
    public ServerUtils setServerPrefix(long gid, @NotNull String prefix) {
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
    public ServerUtils updateServerPrefix(long gid, @NotNull String prefix) {
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
        for (Guild g : new BotUtils().getGuilds())
            prefixes.put(g.getIdLong(), new ServerUtils().getServerPrefix(g.getIdLong()));
    }

    public static String getPrefix(long gid) {
        return prefixes.get(gid);
    }
}
