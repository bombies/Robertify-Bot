package main.utils.locale;

import java.util.HashMap;

public enum RobertifyLocaleMessage {
    ;

    public enum GeneralMessages implements LocaleMessage {
        ON_STATUS,
        OFF_STATUS,
        VOICE_CHANNEL_NEEDED,
        SAME_VOICE_CHANNEL,
        SAME_VOICE_CHANNEL_LOC,
        GENERAL_TOGGLE_MESSAGE,
        NO_VOICE_CHANNEL,
        MUST_BE_PLAYING,
        UNEXPECTED_ERROR,
        ID_GT_ZERO,
        ID_OUT_OF_BOUNDS,
        CANT_JOIN_CHANNEL,
        RESTRICTED_TO_JOIN,
        RESTRICTED_TOGGLED_NO_CHANNELS
    }

    public enum RandomMessages implements LocaleMessage {
        NO_RANDOM_MESSAGES,
        TIP_TITLE,
        TIP_FOOTER
    }

    public enum FilterMessages implements LocaleMessage {
        FILTER_TOGGLE_MESSAGE,
        FILTER_TOGGLE_LOG_MESSAGE
    }

    public enum ClearQueueMessages implements LocaleMessage {
        NOTHING_IN_QUEUE,
        DJ_PERMS_NEEDED,
        QUEUE_CLEARED_USER,
        QUEUE_CLEAR
    }

    public enum DisconnectMessages implements LocaleMessage {
        NOT_IN_CHANNEL,
        DISCONNECTED_USER,
        DISCONNECTED
    }

    public enum FavouriteTracksMessages implements LocaleMessage {
        FT_MISSING_ID,
        FT_INVALID_ID,
        FT_INVALID_SOURCE,
        FAV_TRACK_ADDED,
        FAV_TRACK_REMOVED,
        NO_FAV_TRACKS,
        FAV_TRACKS_CLEARED,
        FT_SELECT_MENU_OPTION,
        FT_ADDING_TO_QUEUE,
        FT_ADDING_TO_QUEUE_2,
        FT_EMBED_TITLE,
        FT_EMBED_FOOTER,
        FT_EMBED_DESCRIPTION
    }

    public static HashMap<Class<? extends LocaleMessage>, LocaleMessage[]> getMessageTypes() {
        final HashMap<Class<? extends LocaleMessage>, LocaleMessage[]> ret = new HashMap<>();
        ret.put(GeneralMessages.class, GeneralMessages.values());
        ret.put(RandomMessages.class, RandomMessages.values());
        ret.put(FilterMessages.class, FilterMessages.values());
        ret.put(ClearQueueMessages.class, ClearQueueMessages.values());
        ret.put(DisconnectMessages.class, DisconnectMessages.values());
        ret.put(FavouriteTracksMessages.class, FavouriteTracksMessages.values());
        return ret;
    }
}
