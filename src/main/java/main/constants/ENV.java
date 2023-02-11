package main.constants;

public enum ENV {
    ENVIRONMENT("environment"),
    GATEWAY_URL("gateway_url"),
    BOT_TOKEN("bot_token"),
    OWNER_ID("owner_id"),
    PREMIUM_BOT("premium_bot"),
    LOAD_COMMANDS("load_commands"),
    SHARD_COUNT("shard_count"),
    VOTE_WEBHOOK_URL("vote_webhook_url"),
    DATABASE_DIR("database_dir"),
    JSON_DIR("json_dir"),
    BOT_NAME("bot_name"),
    BOT_COLOR("bot_color"),
    BOT_SUPPORT_SERVER("bot_support_server"),
    ICON_URL("icon_url"),
    RANDOM_MESSAGE_CHANCE("random_message_chance"),
    YOUTUBE_ENABLED("youtube_enabled"),
    MESSAGE_CONTENT_INTENT_ENABLED("message_content_intent_enabled"),
    PREFIX("prefix"),
    SPOTIFY_CLIENT_ID("spotify_client_id"),
    SPOTIFY_CLIENT_SECRET("spotify_client_secret"),
    DEEZER_ACCESS_TOKEN("deezer_access_token"),
    IMGUR_CLIENT("imgur_client"),
    IMGUR_SECRET("imgur_secret"),
    AUDIO_DIR("local_audio_dir"),
    GENIUS_API_KEY("genius_api_key"),
    MONGO_USERNAME("mongo_username"),
    MONGO_PASSWORD("mongo_password"),
    MONGO_HOSTNAME("mongo_hostname"),
    MONGO_CLUSTER_NAME("mongo_cluster_name"),
    MONGO_DATABASE_NAME("mongo_database_name"),

    POSTGRES_USERNAME("postgres_username"),
    POSTGRES_PASSWORD("postgres_password"),
    POSTGRES_HOST("postgres_host"),
    POSTGRES_PORT("postgres_port"),

    REDIS_HOSTNAME("redis_hostname"),
    REDIS_PORT("redis_port"),
    REDIS_PASSWORD("redis_password"),

    TOP_GG_TOKEN("top_gg_token"),
    DBL_TOKEN("dbl_token"),
    VOTE_REMINDER_CHANCE("vote_reminder_chance"),
    PAPISID("youtube_papisid"),
    PSID("youtube_psid"),

    ROBERTIFY_API_HOSTNAME("robertify_api_hostname"),
    ROBERTIFY_API_PASSWORD("robertify_api_master_password"),

    SENTRY_DSN("sentry_dsn");

    private final String str;

    ENV(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
