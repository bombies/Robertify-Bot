package main.utils.database.sqlite3;

import lombok.Getter;
import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.DatabaseTable;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Deprecated
public class BanDB extends AbstractSQLiteDatabase {
    @Getter
    private static final HashMap<Long, HashMap<Long, Long>> bannedUsers = new HashMap<>();

    public BanDB() {
        super(Database.BANNED_USERS);
    }

    @SneakyThrows
    public BanDB banUser(long gid, long userId, long modId, long bannedAt) {
        openConnectionIfClosed();

        final Statement dbStat = getCon().createStatement();
        final String sql = "INSERT INTO " + DatabaseTable.BANNED_USERS_TABLE + " VALUES(" +
                gid +", "+userId+", "+modId+", "+bannedAt+", null" +
                ");";
        dbStat.executeUpdate(sql);
        return this;
    }

    @SneakyThrows
    public BanDB banUser(long gid, long userId, long modId, long bannedAt, long bannedUntil) {
        if (isUserBannedLazy(gid, userId))
            throw new IllegalArgumentException("User with ID \""+userId+"\" in guild "+gid+" is already banned!");

        openConnectionIfClosed();

        final Statement dbStat = getCon().createStatement();
        final String sql = "INSERT INTO " + DatabaseTable.BANNED_USERS_TABLE + " VALUES(" +
                gid +", "+userId+", "+modId+", "+bannedAt+", "+ bannedUntil +
                ");";
        dbStat.executeUpdate(sql);
        return this;
    }

    @SneakyThrows
    public boolean isUserBanned(long gid, long userId) {
        openConnectionIfClosed();

        final Statement dbStat = getCon().createStatement();
        final String sql = "SELECT * FROM " + DatabaseTable.BANNED_USERS_TABLE + " WHERE guild_id="+gid+" AND banned_id="+userId+";";
        final var resultSet = dbStat.executeQuery(sql);

        final boolean ret = resultSet.isBeforeFirst();
        closeConnection();

        return ret;
    }

    public static boolean isUserBannedLazy(long gid, long userID) {
        return bannedUsers.get(gid).containsKey(userID);
    }

    @SneakyThrows
    public BanDB unbanUser(long gid, long userId) {
        if (!isUserBannedLazy(gid, userId))
            throw new IllegalArgumentException("User with ID \""+userId+"\" in guild "+gid+" isn't banned!");

        openConnectionIfClosed();

        final Statement dbStat = getCon().createStatement();
        final String sql = "DELETE FROM " + DatabaseTable.BANNED_USERS_TABLE + " WHERE guild_id="+gid+" AND banned_id="+userId+";";
        dbStat.executeUpdate(sql);
        return this;
    }

    @SneakyThrows
    public long getWhoBanned(long gid, long userId) {
        if (!isUserBannedLazy(gid, userId))
            throw new IllegalArgumentException("User with ID \""+userId+"\" in guild "+gid+" isn't banned!");

        openConnectionIfClosed();

        final var dbStat = getCon().createStatement();
        final String sql = "SELECT banned_by FROM " + DatabaseTable.BANNED_USERS_TABLE + " WHERE guild_id="+gid+" AND banned_id="+userId+";";
        final var resultSet = dbStat.executeQuery(sql);

        closeConnection();
        while (resultSet.next())
            return resultSet.getLong("banned_by");

        throw new NullPointerException("Something went wrong!");
    }

    @SneakyThrows
    public long getTimeBanned(long gid, long userId) {
        if (!isUserBannedLazy(gid, userId))
            throw new IllegalArgumentException("User with ID \""+userId+"\" in guild "+gid+" isn't banned!");

        openConnectionIfClosed();

        final var statement = getCon().createStatement();
        final String sql = "SELECT banned_at FROM " + DatabaseTable.BANNED_USERS_TABLE + " WHERE guild_id="+gid+" AND banned_id="+userId+";";
        final var resultSet = statement.executeQuery(sql);


        closeConnection();
        while (resultSet.next())
            return resultSet.getLong("banned_by");

        throw new NullPointerException("Something went wrong!");
    }

    @SneakyThrows
    public Long getUnbanTime(long gid, long userId) {
        if (!isUserBannedLazy(gid, userId))
            throw new IllegalArgumentException("User with ID \""+userId+"\" in guild "+gid+" isn't banned!");

        openConnectionIfClosed();

        final var statement = getCon().createStatement();
        final String sql = "SELECT banned_until FROM " + DatabaseTable.BANNED_USERS_TABLE + " WHERE guild_id="+gid+" AND banned_id="+userId+";";
        final var resultSet = statement.executeQuery(sql);



        Long ret = null;
        while (resultSet.next())
            ret = resultSet.getObject("banned_until") != null ? resultSet.getLong("banned_until") : -1L;

        closeConnection();
        return ret;
    }

    public long getTimeUntilUnban(long gid, long userId) {
        final long unbanTime = getUnbanTime(gid, userId);

        if (unbanTime == -1L)
            return -1L;

        final long bannedTime = getTimeBanned(gid, userId);
        return unbanTime - bannedTime;
    }

    @SneakyThrows
    public HashMap<Long, Long> getAllBannedUsers(long gid) {
        HashMap<Long, Long> ret = new HashMap<>();

        openConnectionIfClosed();

        final var statement = getCon().createStatement();
        final String sql = "SELECT banned_id, banned_at, banned_until FROM " + DatabaseTable.BANNED_USERS_TABLE + " WHERE guild_id="+gid+";";
        final var resultSet = statement.executeQuery(sql);

        while (resultSet.next()) {
            HashMap<Long, Long> map = new HashMap<>();
            final long bannedId = resultSet.getLong("banned_id");
            final Long bannedUntil = resultSet.getObject("banned_until") == null ? null : resultSet.getLong("banned_until");

            ret.put(bannedId, bannedUntil);
        }

        closeConnection();
        return ret;
    }

    @SneakyThrows
    public List<Long> getBannedUserIDs(Long gid) {
        List<Long> ret = new ArrayList<>();

        openConnectionIfClosed();

        final var statement = getCon().createStatement();
        final String sql = "SELECT banned_id FROM " + DatabaseTable.BANNED_USERS_TABLE + " WHERE guild_id="+gid+";";
        final var resultSet = statement.executeQuery(sql);

        while (resultSet.next()) {
            final long bannedId = resultSet.getLong("banned_id");
            ret.add(bannedId);
        }

        closeConnection();
        return ret;
    }

    public static void initBannedUserMap() {
        for (Guild g : Robertify.api.getGuilds()) {
            var bannedUserIDs = new BanDB().getAllBannedUsers(g.getIdLong());
            bannedUsers.put(g.getIdLong(), bannedUserIDs);
        }
    }

    public static void addBannedUser(long gid, long userId, Long bannedUntil) {
        bannedUsers.get(gid).put(userId, bannedUntil);
    }

    public static void removeBannedUser(long gid, Long userId) {
        bannedUsers.get(gid).remove(userId);
    }
}
