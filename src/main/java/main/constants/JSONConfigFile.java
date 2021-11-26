package main.constants;

public enum JSONConfigFile {
    PERMISSIONS("permissions.json"),
    CHANGELOG("changelog.json"),
    TOGGLES("toggles.json"),
    DEDICATED_CHANNELS("dedicatedchannels.json"),
    EIGHT_BALL("8ball.json");

    private final String str;

    JSONConfigFile(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
