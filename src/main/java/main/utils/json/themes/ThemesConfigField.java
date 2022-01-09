package main.utils.json.themes;

import main.utils.json.GenericJSONField;

public enum ThemesConfigField implements GenericJSONField {
    THEME("theme");

    private final String str;

    ThemesConfigField(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
