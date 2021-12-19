package main.utils.json.changelog;

import main.utils.json.GenericJSONField;

public enum ChangeLogConfigField implements GenericJSONField {
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
