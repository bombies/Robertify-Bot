package main.constants;

public enum JSONConfigFile {
    PERMISSIONS("permissions.json"),
    CHANGELOG("changelog.json");

    private final String str;

    JSONConfigFile(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
