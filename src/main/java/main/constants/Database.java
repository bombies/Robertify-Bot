package main.constants;

import main.main.Config;

public enum Database {
    MAIN("main"),
    BANNED_USERS("bannedusers"),
    TRACKS_PLAYED("tracks");

    private final String str;

    Database(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }

    public enum MONGO {
        MAIN("Learning0"),

        ROBERTIFY_DATABASE(Config.get(ENV.MONGO_DATABASE_NAME)),

        // Collections
        ROBERTIFY_BOT_INFO("main"),
        ROBERTIFY_GUILDS("guilds"),
        ROBERTIFY_PERMISSIONS("permissions"),
        ROBERTIFY_TEST("test");

        private final String str;

        MONGO(String str) {
            this.str =str;
        }

        @Override
        public String toString() {
            return str;
        }

        public static String getConnectionString(String db) {

            return "mongodb+srv://" + Config.get(ENV.MONGO_USERNAME) + ":" + Config.get(ENV.MONGO_PASSWORD) + "@"+Config.get(ENV.MONGO_CLUSTER_NAME)+"."+Config.get(ENV.MONGO_HOSTNAME)+"/" + db + "?retryWrites=true&w=majority";
        }
    }
}
