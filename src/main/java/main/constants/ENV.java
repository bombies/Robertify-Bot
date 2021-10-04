package main.constants;

public enum ENV {
    BOT_TOKEN("bot_token"),
    DATABASE_DIR("database_dir"),
    JSON_DIR("json_dir"),
    PREFIX("prefix"),
    SPOTIFY_CLIENT_ID("spotify_client_id"),
    SPOTIFY_CLIENT_SECRET("spotify_client_secret");

    private final String str;

    ENV(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
