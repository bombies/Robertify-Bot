package main.utils.json.requestchannel;

public enum RequestChannelConfigField {
    CHANNEL_ID("channel_id"),
    QUEUE_MESSAGE_ID("message_id"),
    ORIGINAL_ANNOUNCEMENT_TOGGLE("og_announcement_toggle");

    private final String str;

    RequestChannelConfigField(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
