package main.utils.database.sqlite3;

import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.DatabaseTable;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class BotDB extends AbstractSQLiteDatabase {

    public BotDB() {
        super(Database.MAIN);
    }

    /**
     * Add a guild to the database
     * @param gid ID of the guild
     */
    @SneakyThrows
    public BotDB addGuild(long gid) {
        if (getCon().isClosed())
            createConnection();

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
    public BotDB removeGuild(long gid) {
        if (getCon().isClosed())
            createConnection();

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
        if (getCon().isClosed())
            createConnection();

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
        if (getCon().isClosed())
            createConnection();

        Statement dbStat = getCon().createStatement();
        String sql = "SELECT * FROM " + DatabaseTable.MAIN_BOT_DEVELOPERS + " WHERE developer_id='"+id+"';";
        ResultSet dbRes = dbStat.executeQuery(sql);

        String ret = null;
        while (dbRes.next()) ret = dbRes.getString("developer_id");

        getCon().close();
        return ret != null;
    }

    @SneakyThrows
    public BotDB addDeveloper(String id) {
        if (getCon().isClosed())
            createConnection();

        if (isDeveloper(id))
            throw new NullPointerException("The ID passed already belongs to a developer");

        createConnection();

        Statement dbStat = getCon().createStatement();
        String sql = "INSERT INTO " + DatabaseTable.MAIN_BOT_DEVELOPERS + " VALUES('"+id+"');";
        dbStat.executeUpdate(sql);
        return this;
    }

    @SneakyThrows
    public BotDB removeDeveloper(String id) {
        if (getCon().isClosed())
            createConnection();

        if (!isDeveloper(id))
            throw new NullPointerException("The ID passed doesn't belong to a developer");

        createConnection();

        Statement dbStat = getCon().createStatement();
        String sql = "DELETE FROM " + DatabaseTable.MAIN_BOT_DEVELOPERS + " WHERE developer_id='"+id+"';";
        dbStat.executeUpdate(sql);
        return this;
    }

    /**
     * Get whether the bot should announce new tracks being played
     * @return True if the track is to be announced and vice versa.
     */
    @SneakyThrows
    public boolean announceNewTrack(long guildID) {
        if (getCon().isClosed())
            createConnection();

        Statement dbStat = getCon().createStatement();
        String sql = "SELECT * FROM " + DatabaseTable.MAIN_BOT_INFO + " WHERE server_id="+guildID+";";
        ResultSet dbRes = dbStat.executeQuery(sql);

         boolean ret = true;

         while (dbRes.next()) ret = dbRes.getBoolean("announce_msgs");

         getCon().close();
         return ret;
    }

    /**
     * Set the boolean value for whether the bot should announce new tracks being played.
     * @param gid The ID of the guild
     * @param value The boolean value
     * @return A copy of this object to continue chains
     */
    @SneakyThrows
    public BotDB announceNewTrack(long gid, boolean value) {
        if (getCon().isClosed())
            createConnection();

        Statement dbStat = getCon().createStatement();
        String sql = "UPDATE " + DatabaseTable.MAIN_BOT_INFO + " SET announce_msgs="+value+"" +
                " WHERE server_id="+gid+";";
        dbStat.executeUpdate(sql);
        return this;
    }

    @SneakyThrows
    public BotDB setAnnouncementChannel(long guildID, long channelID) {
        if (getCon().isClosed())
            createConnection();

        Statement dbStat = getCon().createStatement();
        String sql = "UPDATE " + DatabaseTable.MAIN_BOT_INFO + " SET announcement_channel="+channelID+"" +
                " WHERE server_id="+guildID+";";
        dbStat.executeUpdate(sql);
        return this;
    }

    @SneakyThrows
    public long getAnnouncementChannel(long guildID) {
        if (getCon().isClosed())
            createConnection();

        Statement dbStat = getCon().createStatement();
        String sql = "SELECT * FROM " + DatabaseTable.MAIN_BOT_INFO +
                " WHERE server_id="+guildID+";";
        ResultSet dbRes = dbStat.executeQuery(sql);

        long ret = -1;

        while (dbRes.next()) ret = dbRes.getLong("announcement_channel");

        getCon().close();
        return ret;
    }

    @SneakyThrows
    public TextChannel getAnnouncementChannelObject(long guildID) {
        return Robertify.api.getTextChannelById(getAnnouncementChannel(guildID));
    }

    /**
     * Checks if the announcement channel has been set
     * @param gid ID of the guild
     * @return True if the channel is set and vice versa.
     */
    public boolean isAnnouncementChannelSet(long gid) {
        return getAnnouncementChannel(gid) != 0;
    }

    public void createConnection() {
        super.createConnection(Database.MAIN);
    }
}
