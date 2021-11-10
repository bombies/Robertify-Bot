package main.constants;

public enum ENV {
    BOT_TOKEN("bot_token"),
    DATABASE_DIR("database_dir"),
    JSON_DIR("json_dir"),
    BOT_NAME("bot_name"),
    BOT_COLOR("bot_color"),
    ICON_URL("icon_url"),
    PREFIX("prefix"),
    SPOTIFY_CLIENT_ID("spotify_client_id"),
    SPOTIFY_CLIENT_SECRET("spotify_client_secret"),
    IMGUR_CLIENT("imgur_client"),
    IMGUR_SECRET("imgur_secret");

    private final String str;

    ENV(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
