package main.utils.votes.api.discordbotlist;

import java.util.concurrent.CompletionStage;

public interface DBLApi {
    CompletionStage<Void> setStats(int guilds);

    public static class Builder {
        private String botID;
        private String token;

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setBotID(String botID) {
            this.botID = botID;
            return this;
        }

        public DBLApi build() {
            if (botID == null)
                throw new IllegalArgumentException("This bot ID can't be null!");
            if (token == null)
                throw new IllegalArgumentException("The API token can't be null!");

            return new DBLApiImpl(token, botID);
        }

    }
}
