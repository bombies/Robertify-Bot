package main.utils.locale;

import java.util.HashMap;

public enum RobertifyLocaleMessage {
    ;

    public enum GeneralMessages implements LocaleMessage {
        ON_STATUS,
        OFF_STATUS,
        INVALID_ARGS,
        VOICE_CHANNEL_NEEDED,
        USER_VOICE_CHANNEL_NEEDED,
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
        RESTRICTED_TOGGLED_NO_CHANNELS,
        INSUFFICIENT_PERMS,
        DJ_ONLY,
        NOTHING_PLAYING
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

    public enum JoinMessages implements LocaleMessage {
        ALREADY_JOINED,
        CANT_JOIN,
        JOINED
    }

    public enum JumpMessages implements LocaleMessage {
        JUMP_MISSING_AMOUNT,
        JUMP_INVALID_DURATION,
        JUMP_DURATION_NEG_ZERO,
        JUMP_DURATION_GT_TIME_LEFT,
        JUMPED_LOG,
        JUMPED
    }

    public enum LofiMessages implements LocaleMessage {
        LOFI_ENABLING,
        LOFI_DISABLED
    }

    public enum LoopMessages implements LocaleMessage {
        LOOP_NOTHING_PLAYING,
        LOOP_STOP,
        LOOP_START,
        LOOP_LOG,
        QUEUE_LOOP_STOP,
        QUEUE_LOOP_START,
        QUEUE_LOOP_NOTHING,
        QUEUE_LOOP_LOG
    }

    public static HashMap<Class<? extends LocaleMessage>, LocaleMessage[]> getMessageTypes() {
        final HashMap<Class<? extends LocaleMessage>, LocaleMessage[]> ret = new HashMap<>();
        ret.put(GeneralMessages.class, GeneralMessages.values());
        ret.put(RandomMessages.class, RandomMessages.values());
        ret.put(FilterMessages.class, FilterMessages.values());
        ret.put(ClearQueueMessages.class, ClearQueueMessages.values());
        ret.put(DisconnectMessages.class, DisconnectMessages.values());
        ret.put(FavouriteTracksMessages.class, FavouriteTracksMessages.values());
        ret.put(JoinMessages.class, JoinMessages.values());
        ret.put(JumpMessages.class, JumpMessages.values());
        ret.put(LofiMessages.class, LofiMessages.values());
        ret.put(LoopMessages.class, LoopMessages.values());
        return ret;
    }
}
