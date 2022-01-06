package main.constants;

public enum Statistic {
    COMMANDS_USED("commands_used");

    private final String str;

    Statistic(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
