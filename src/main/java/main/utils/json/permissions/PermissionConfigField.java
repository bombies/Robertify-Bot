package main.utils.json.permissions;

import main.utils.json.AbstractJSONConfig;
import main.utils.json.IJSONField;

public enum PermissionConfigField implements IJSONField {
    USER_PERMISSIONS("users");

    private final String str;

    PermissionConfigField(String str) {
        this.str =str;
    }

    @Override
    public String toString() {
        return str;
    }
}
