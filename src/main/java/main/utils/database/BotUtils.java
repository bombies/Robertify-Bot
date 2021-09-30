package main.utils.database;

import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.DatabaseTable;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BotUtils extends DatabaseUtils{
    public BotUtils() {
        super(Database.MAIN);
    }

    /**
     * Add a guild to the database
     * @param gid ID of the guild
     */
    @SneakyThrows
    public BotUtils addGuild(long gid) {
        Statement dbStat = getCon().createStatement();
        String sql = "INSERT INTO " + DatabaseTable.MAIN_BOT_INFO + "(server_id, prefix) VALUES(" +
                ""+gid+"," +
                "'"+Config.get(ENV.PREFIX)+"'" +
                ");";
        dbStat.executeUpdate(sql);
        return this;
    }

    /**
     * Remove a guild from the database
     * @param gid ID of the guild
     */
    @SneakyThrows
    public BotUtils removeGuild(long gid) {
        Statement dbStat = getCon().createStatement();
        String sql = "DELETE FROM " + DatabaseTable.MAIN_BOT_INFO + " WHERE server_id="+gid+";";
        dbStat.executeUpdate(sql);
        return this;
    }

    /**
     * Get a list of all the guilds the bot is in
     * @return ArrayList of all the guilds the but is currently active in.
     */
    @SneakyThrows
    public List<Guild> getGuilds() {
        Statement dbStat = getCon().createStatement();
        String sql = "SELECT server_id FROM " + DatabaseTable.MAIN_BOT_INFO + ";";
        ResultSet dbRes = dbStat.executeQuery(sql);

        List<Long> guildIDs = new ArrayList<>();

        while (dbRes.next())
            guildIDs.add(dbRes.getLong("server_id"));

        List<Guild> guilds = new ArrayList<>();

        for (long gid : guildIDs)
            guilds.add(Robertify.api.getGuildById(gid));

        getCon().close();
        return guilds;
    }

    /**
     * Checks if the ID passed belongs to one of the bot's developers.
     * @param id Discord ID of the user
     * @return True - if the value of the fetched string isn't null.
     *          False - Vice versa.
     * @throws SQLException
     */
    public boolean isDeveloper(String id) throws SQLException {
        Statement dbStat = getCon().createStatement();
        String sql = "SELECT * FROM " + DatabaseTable.MAIN_BOT_DEVELOPERS + " WHERE developer_id='"+id+"';";
        ResultSet dbRes = dbStat.executeQuery(sql);

        String ret = null;
        while (dbRes.next()) ret = dbRes.getString("developer_id");

        getCon().close();
        return ret != null;
    }
}
