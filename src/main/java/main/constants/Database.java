package main.constants;

public enum Database {
    MAIN("main"),
    BANNED_USERS("bannedusers");

    private final String str;

    Database(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
