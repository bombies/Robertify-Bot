package main.constants;

public enum ENV {
    BOT_TOKEN("bot_token"),
    DATABASE_DIR("database_dir"),
    PREFIX("prefix");

    private final String str;

    ENV(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
