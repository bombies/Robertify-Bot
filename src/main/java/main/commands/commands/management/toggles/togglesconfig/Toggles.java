package main.commands.commands.management.toggles.togglesconfig;

import main.utils.json.IJSONField;

public enum Toggles implements IJSONField {
    ANNOUNCE_MESSAGES("announce_messages"),
    ANNOUNCE_CHANGELOGS("announce_changelogs"),
    SHOW_REQUESTER("show_requester");

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
        }
        throw new NullPointerException("No such toggle!");
    }

    enum TogglesConfigField implements IJSONField {
        DJ_TOGGLES("dj_toggles");

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
