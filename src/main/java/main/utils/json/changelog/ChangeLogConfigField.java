package main.utils.json.changelog;

import main.utils.json.IJSONField;

public enum ChangeLogConfigField implements IJSONField {
    CURRENT_LOG("current_log"),
    PAST_LOGS("past_logs"),
    LOGS("logs");

    private String str;

    ChangeLogConfigField(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
