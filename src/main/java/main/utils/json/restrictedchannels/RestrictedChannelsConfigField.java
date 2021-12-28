package main.utils.json.restrictedchannels;

import main.utils.json.GenericJSONField;

public enum RestrictedChannelsConfigField implements GenericJSONField {
    VOICE_CHANNELS("voice_channels"),
    TEXT_CHANNELS("text_channels");

    private final String str;

    RestrictedChannelsConfigField(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
