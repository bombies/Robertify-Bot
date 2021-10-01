package main.constants;

public enum DatabaseTable {
    MAIN_BOT_INFO("bot_info"),
    MAIN_BOT_DEVELOPERS("bot_developers");

    private String str;

    DatabaseTable(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
