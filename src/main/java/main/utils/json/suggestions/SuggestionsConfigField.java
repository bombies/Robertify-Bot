package main.utils.json.suggestions;

import main.utils.json.GenericJSONField;

public enum SuggestionsConfigField implements GenericJSONField {
    SUGGESTIONS_CATEGORY("suggestions_category"),
    PENDING_CHANNEL("pending_channel"),
    ACCEPTED_CHANNEL("accepted_channel"),
    DENIED_CHANNEL("denied_channel"),
    BANNED_USERS("banned_users");

    private final String str;

    SuggestionsConfigField(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
