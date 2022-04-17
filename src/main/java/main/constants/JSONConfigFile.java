package main.constants;

public enum JSONConfigFile {
    CHANGELOG("changelog.json"),
    RESUME_DATA("resume_data.json");

    private final String str;

    JSONConfigFile(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
