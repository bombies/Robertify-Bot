package main.utils.database.postgresql.voters;

import lombok.SneakyThrows;
import main.utils.database.postgresql.AbstractPostgresTable;
import main.utils.votes.VoteManager;

import java.util.concurrent.TimeUnit;

public class VoterTable extends AbstractPostgresTable {
    public final static String TABLE_NAME = "latest_votes";
    private final VoterDB database;

    protected VoterTable(VoterDB database) {
        super(database.getConnection(), TABLE_NAME);
        this.database = database;
    }

    @SneakyThrows
    public void setLastVote(long uid, long time, VoteManager.Website website) {
        final String sql;
        final String columnToUse;

        switch (website) {
            case TOP_GG -> columnToUse = Fields.LAST_VOTED_TOP_GG.toString();
            case DBL -> columnToUse = Fields.LAST_VOTED_DBL.toString();
            default -> throw new IllegalArgumentException("How did you provide an invalid ENUM!?!?!");
        }

        if (!userExists(uid)) {
            sql = "INSERT INTO " + TABLE_NAME + "(" + Fields.VOTER_ID + ", " + columnToUse + ") VALUES(" +
                    uid + ", " + time +
                    ");";
        } else {
            sql = "UPDATE " + TABLE_NAME +
                    " SET " + columnToUse + "=" + time +
                    " WHERE " + Fields.VOTER_ID + "=" + uid+ ";";
        }

        getConnection().createStatement().executeUpdate(sql);
    }

    @SneakyThrows
    public long getLastVote(long uid, VoteManager.Website website) {
        if (!userExists(uid))
            throw new IllegalArgumentException("This user has never voted!");
        final var sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + Fields.VOTER_ID + "=" + uid + ";";
        final var resultSet = getResultSet(sql);

        if (resultSet.first())
            return website.equals(VoteManager.Website.TOP_GG) ? resultSet.getLong(1) : resultSet.getLong(2);
        throw new Exception("How even??");
    }

    @SneakyThrows
    public boolean userHasVoted(long uid, VoteManager.Website website) {
        if (!userExists(uid))
            return false;

        final var sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + Fields.VOTER_ID + "=" + uid + ";";
        final var resultSet = getResultSet(sql);

        if (resultSet.first()) {
            final String columnToUse;

            switch (website) {
                case TOP_GG -> columnToUse = Fields.LAST_VOTED_TOP_GG.toString();
                case DBL -> columnToUse = Fields.LAST_VOTED_DBL.toString();
                default -> columnToUse = null;
            }

            return resultSet.getLong(columnToUse) < TimeUnit.HOURS.toMillis(12);
        }
        throw new Exception("How even??");
    }

    @SneakyThrows
    private boolean userExists(long uid) {
        final var sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + Fields.VOTER_ID + "=" + uid + ";";
        final var resultSet = getResultSet(sql);
        return resultSet.next();
    }

    @Override @SneakyThrows
    public void init() {
        final String sql = "CREATE TABLE " + getTableName() + "(" +
                    "" + Fields.VOTER_ID + " bigserial PRIMARY KEY," +
                    "" + Fields.LAST_VOTED_TOP_GG + " bigserial," +
                    "" + Fields.LAST_VOTED_DBL + " bigserial" +
                ");";
        getConnection().createStatement().execute(sql);
    }

    private enum Fields {
        VOTER_ID("voter_id"),
        LAST_VOTED_TOP_GG("last_voted_tgg"),
        LAST_VOTED_DBL("last_voted_dbl");

        private final String str;

        Fields(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
