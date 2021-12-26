package main.utils.json.reports;

import main.utils.json.GenericJSONField;

public enum ReportsConfigField implements GenericJSONField {
    CATEGORY("reports_category"),
    CHANNEL("reports_channel"),
    BANNED_USERS("banned_users");

    private final String str;

    ReportsConfigField(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
