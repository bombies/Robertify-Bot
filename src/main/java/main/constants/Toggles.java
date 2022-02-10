package main.constants;

import main.utils.json.GenericJSONField;

public enum Toggles implements GenericJSONField {
    RESTRICTED_VOICE_CHANNELS("restricted_voice_channels"),
    RESTRICTED_TEXT_CHANNELS("restricted_text_channels"),
    ANNOUNCE_MESSAGES("announce_messages"),
    ANNOUNCE_CHANGELOGS("announce_changelogs"),
    GLOBAL_ANNOUNCEMENTS("global_announcements"),
    SHOW_REQUESTER("show_requester"),
    EIGHT_BALL("8ball"),
    POLLS("polls"),
    TIPS("tips");

    private final String str;

    Toggles(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }

    public static String parseToggle(Toggles toggle) {
        switch (toggle) {
            case ANNOUNCE_MESSAGES -> {
                return "announcements";
            }
            case ANNOUNCE_CHANGELOGS -> {
                return "changelogs";
            }
            case SHOW_REQUESTER -> {
                return "requester";
            }
            case EIGHT_BALL -> {
                return "8ball";
            }
            case POLLS -> {
                return "polls";
            }
            case RESTRICTED_VOICE_CHANNELS -> {
                return "restrictedvoice";
            }
            case RESTRICTED_TEXT_CHANNELS -> {
                return "restrictedtext";
            }
            case GLOBAL_ANNOUNCEMENTS -> {
                return "globalannouncements";
            }
            case TIPS -> {
                return "tips";
            }
        }
        throw new NullPointerException("No such toggle!");
    }

    public enum TogglesConfigField implements GenericJSONField {
        DJ_TOGGLES("dj_toggles"),
        LOG_TOGGLES("log_toggles");

        private final String str;

        TogglesConfigField(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
