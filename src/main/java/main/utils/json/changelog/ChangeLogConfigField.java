package main.utils.json.changelog;

import main.utils.json.GenericJSONField;

@Deprecated
public enum ChangeLogConfigField implements GenericJSONField {
    CURRENT_LOG("current_log"),
    PAST_LOGS("past_logs"),
    TITLE("title"),
    LOGS("logs"),
    LOG_TYPE("log_type"),
    LOG_STRING("log_string");

    private final String str;

    ChangeLogConfigField(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
