package main.constants;

public enum ENV {
    BOT_TOKEN("bot_token"),
    OWNER_ID("owner_id"),
    DATABASE_DIR("database_dir"),
    JSON_DIR("json_dir"),
    BOT_NAME("bot_name"),
    BOT_COLOR("bot_color"),
    BOT_SUPPORT_SERVER("bot_support_server"),
    ICON_URL("icon_url"),
    RANDOM_MESSAGE_CHANCE("random_message_chance"),
    PREFIX("prefix"),
    LAVALINK_NODE("lavalink_node"),
    LAVALINK_NODE_PASSWORD("lavalink_node_password"),
    SPOTIFY_CLIENT_ID("spotify_client_id"),
    SPOTIFY_CLIENT_SECRET("spotify_client_secret"),
    YOUTUBE_PAPISID("youtube_papisid"),
    YOUTUBE_PSID("youtube_psid"),
    IMGUR_CLIENT("imgur_client"),
    IMGUR_SECRET("imgur_secret"),
    AUDIO_DIR("local_audio_dir"),
    GENIUS_API_KEY("genius_api_key"),
    MONGO_USERNAME("mongo_username"),
    MONGO_PASSWORD("mongo_password"),
    MONGO_HOSTNAME("mongo_hostname"),
    MONGO_CLUSTER_NAME("mongo_cluster_name"),
    MONGO_DATABASE_NAME("mongo_database_name"),
    TOP_GG_TOKEN("top_gg_token"),
    DBL_TOKEN("dbl_token"),
    VOTE_REMINDER_CHANCE("vote_reminder_chance");

    private final String str;

    ENV(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
