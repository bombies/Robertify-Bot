package main.constants;

import main.utils.json.GenericJSONField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Toggles implements GenericJSONField {
    RESTRICTED_VOICE_CHANNELS("restricted_voice_channels"),
    RESTRICTED_TEXT_CHANNELS("restricted_text_channels"),
    ANNOUNCE_MESSAGES("announce_messages"),
    SHOW_REQUESTER("show_requester"),
    EIGHT_BALL("8ball"),
    POLLS("polls"),
    REMINDERS("reminders"),
    TIPS("tips"),
    VOTE_SKIPS("vote_skips");

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
            case SHOW_REQUESTER -> {
                return "requester";
            }
            case EIGHT_BALL -> {
                return "8ball";
            }
            case POLLS -> {
                return "polls";
            }
            case REMINDERS -> {
                return "reminders";
            }
            case RESTRICTED_VOICE_CHANNELS -> {
                return "restrictedvoice";
            }
            case RESTRICTED_TEXT_CHANNELS -> {
                return "restrictedtext";
            }
            case TIPS -> {
                return "tips";
            }
            case VOTE_SKIPS -> {
                return "voteskips";
            }
        }
        throw new NullPointerException("No such toggle!");
    }

    public static List<String> toList() {
        return Arrays.stream(Toggles.values())
                .map(Toggles::parseToggle)
                .toList();
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
