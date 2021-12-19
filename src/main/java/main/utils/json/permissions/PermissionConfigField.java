package main.utils.json.permissions;

import main.utils.json.GenericJSONField;

public enum PermissionConfigField implements GenericJSONField {
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
