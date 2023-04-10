package main.utils.locale.messages

import main.utils.locale.LocaleMessageKt
import kotlin.reflect.KClass

sealed class RobertifyLocaleMessageKt {
    enum class GeneralMessages : LocaleMessageKt {
        ON_STATUS,
        OFF_STATUS,
        INVALID_ARGS,
        INVALID_ARGS_USAGES,
        INSUFFICIENT_ARGS,
        INSUFFICIENT_ARGS_USAGES,
        VOICE_CHANNEL_NEEDED,
        USER_VOICE_CHANNEL_NEEDED,
        SAME_VOICE_CHANNEL,
        SAME_VOICE_CHANNEL_LOC,
        SAME_VOICE_CHANNEL_BUTTON,
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
        INSUFFICIENT_PERMS_NO_ARGS,
        SELF_INSUFFICIENT_PERMS,
        SELF_INSUFFICIENT_PERMS_ARGS,
        DJ_ONLY,
        DJ_BUTTON,
        NOTHING_PLAYING,
        NOTHING_IN_QUEUE,
        CANT_BE_USED_IN_CHANNEL,
        CANT_BE_USED_IN_CHANNEL_ARGS,
        NO_MENU_PERMS,
        NO_PERMS_END_INTERACTION,
        NO_PERMS_BUTTON,
        BUTTON_NO_LONGER_VALID,
        NO_SUCH_CHANNEL,
        OK,
        NOTHING_HERE,
        INVALID_MINUTE,
        INVALID_HOUR,
        MUST_PROVIDE_VALID_CHANNEL,
        SELECT_MENU_PLACEHOLDER,
        VOTE_EMBED_DESC,
        WEBSITE_EMBED_DESC,
        INSUFFICIENT_PERMS_TO_JOIN,
        UNKNOWN_REQUESTER,
        BANNED_FROM_COMMANDS,
        NO_EMBED_PERMS,
        UNREAD_ALERT,
        UNREAD_ALERT_MENTION,
        DISABLED_FEATURE,
        PREMIUM_INSTANCE_NEEDED,
        PREMIUM_EMBED_TITLE,
        PREMIUM_UPGRADE_BUTTON,
        NO_YOUTUBE_SUPPORT,
        DISABLED_COMMAND,
        GUILD_COMMAND_ONLY;
    }

    enum class RandomMessages : LocaleMessageKt {
        NO_RANDOM_MESSAGES,
        TIP_TITLE,
        TIP_FOOTER
    }

    enum class FilterMessages : LocaleMessageKt {
        FILTER_TOGGLE_MESSAGE,
        FILTER_TOGGLE_LOG_MESSAGE,
        FILTER_SELECT_PLACEHOLDER,
        EIGHT_D,
        KARAOKE,
        NIGHTCORE,
        TREMOLO,
        VIBRATO
    }

    enum class ClearQueueMessages : LocaleMessageKt {
        CQ_NOTHING_IN_QUEUE,
        DJ_PERMS_NEEDED,
        QUEUE_CLEARED_USER,
        QUEUE_CLEAR
    }

    enum class DisconnectMessages : LocaleMessageKt {
        NOT_IN_CHANNEL,
        DISCONNECTED_USER,
        DISCONNECTED
    }

    enum class FavouriteTracksMessages : LocaleMessageKt {
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


    enum class JoinMessages : LocaleMessageKt {
        ALREADY_JOINED,
        CANT_JOIN,
        JOINED
    }


    enum class JumpMessages : LocaleMessageKt {
        JUMP_MISSING_AMOUNT,
        JUMP_INVALID_DURATION,
        JUMP_DURATION_NEG_ZERO,
        JUMP_DURATION_GT_TIME_LEFT,
        JUMPED_LOG,
        JUMPED
    }


    enum class LofiMessages : LocaleMessageKt {
        LOFI_ENABLING,
        LOFI_ENABLED,
        LOFI_DISABLED
    }


    enum class LoopMessages : LocaleMessageKt {
        LOOP_NOTHING_PLAYING,
        LOOP_STOP,
        LOOP_START,
        LOOP_LOG,
        QUEUE_LOOP_STOP,
        QUEUE_LOOP_START,
        QUEUE_LOOP_NOTHING,
        QUEUE_LOOP_LOG
    }


    enum class LyricsMessages : LocaleMessageKt {
        LYRICS_SOURCE_NOT_SUPPORTED,
        LYRICS_SEARCHING,
        LYRICS_NOW_SEARCHING,
        LYRICS_NOTHING_FOUND,
        LYRICS_EMBED_TITLE
    }


    enum class MoveMessages : LocaleMessageKt {
        INVALID_SONG_ID,
        INVALID_POSITION_ID,
        COULDNT_MOVE,
        MOVED_LOG,
        MOVED
    }


    enum class NowPlayingMessages : LocaleMessageKt {
        NP_LOFI_TITLE,
        NP_EMBED_TITLE,
        NP_REQUESTER,
        NP_LIVESTREAM,
        NP_TIME_LEFT,
        NP_AUTHOR,
        NP_ANNOUNCEMENT_DESC,
        NP_ANNOUNCEMENT_REQUESTER
    }


    enum class PauseMessages : LocaleMessageKt {
        PAUSED,
        PAUSED_LOG,
        RESUMED,
        RESUMED_LOG,
        PLAYER_NOT_PAUSED
    }


    enum class PlayMessages : LocaleMessageKt {
        MISSING_ARGS,
        MISSING_FILE,
        LOCAL_DIR_ERR,
        FILE_DOWNLOAD_ERR,
        INVALID_FILE
    }


    enum class PreviousTrackMessages : LocaleMessageKt {
        NO_PREV_TRACKS,
        PLAYING_PREV_TRACK,
        PREV_TRACK_LOG
    }


    enum class QueueMessages : LocaleMessageKt {
        QUEUE_ENTRY
    }


    enum class RemoveMessages : LocaleMessageKt {
        REMOVE_INVALID_ID,
        REMOVED,
        REMOVED_LOG,
        COULDNT_REMOVE
    }


    enum class RewindMessages : LocaleMessageKt {
        INVALID_DURATION,
        CANT_REWIND_STREAM,
        REWIND_TO_BEGINNING,
        REWIND_TO_BEGINNING_LOG,
        DURATION_GT_CURRENT_TIME,
        REWOUND_BY_DURATION,
        REWOUND_BY_DURATION_LOG
    }


    enum class SearchMessages : LocaleMessageKt {
        MUST_PROVIDE_QUERY,
        LOOKING_FOR,
        SEARCH_MENU_PLACEHOLDER,
        SEARCH_EMBED_AUTHOR,
        SEARCH_EMBED_FOOTER,
        SEARCH_END_INTERACTION
    }


    enum class SeekMessages : LocaleMessageKt {
        INVALID_MINUTES,
        INVALID_SECONDS,
        POS_GT_DURATION,
        SOUGHT,
        SOUGHT_LOG
    }


    enum class ShufflePlayMessages : LocaleMessageKt {
        MISSING_LINK,
        INVALID_LINK,
        NOT_PLAYLIST,
        MUST_PROVIDE_VALID_PLAYLIST
    }


    enum class ShuffleMessages : LocaleMessageKt {
        SHUFFLED,
        SHUFFLED_LOG
    }


    enum class SkipMessages : LocaleMessageKt {
        VOTE_SKIP_STARTED,
        VOTE_SKIP_CANCELLED,
        VOTE_SKIP_STARTED_LOG,
        VOTE_SKIP_STARTED_EMBED,
        NOTHING_TO_SKIP,
        SKIPPED,
        SKIPPED_LOG,
        VOTE_SKIPPED,
        VOTE_SKIPPED_LOG,
        SKIP_VOTE_ADDED,
        SKIP_VOTE_REMOVED,
        DJ_SKIPPED
    }


    enum class StopMessages : LocaleMessageKt {
        STOPPED,
        STOPPED_LOG
    }


    enum class VolumeMessages : LocaleMessageKt {
        INVALID_VOLUME,
        VOLUME_CHANGED,
        VOLUME_CHANGED_LOG
    }


    enum class BanMessages : LocaleMessageKt {
        BAN_INVALID_USER,
        BAN_INVALID_USER_DETAILED,
        INVALID_BAN_DURATION,
        CANNOT_BAN_ADMIN,
        CANNOT_BAN_DEVELOPER,
        USER_ALREADY_BANNED,
        USER_PERM_BANNED,
        USER_PERM_BANNED_RESPONSE,
        USER_TEMP_BANNED,
        USER_TEMP_BANNED_RESPONSE
    }


    enum class UnbanMessages : LocaleMessageKt {
        MISSING_UNBAN_USER,
        INVALID_UNBAN_USER,
        USER_NOT_BANNED,
        USER_UNBANNED,
        USER_UNBANNED_RESPONSE
    }


    enum class LogChannelMessages : LocaleMessageKt {
        LOG_CHANNEL_ALREADY_SETUP,
        LOG_CHANNEL_SUCCESSFUL_SETUP,
        LOG_CHANNEL_MISSING,
        INVALID_LOG_CHANNEL,
        CANNOT_SET_LOG_CHANNEL,
        LOG_CHANNEL_SET,
        LOG_CHANNEL_INVALID_TYPE
    }


    enum class RestrictedChannelMessages : LocaleMessageKt {
        MISSING_RESTRICTED_CHANNEL,
        INVALID_RESTRICTED_CHANNEL_ID,
        RESTRICTED_CHANNELS_TOGGLED_OFF,
        RESTRICTED_CHANNEL_ID_INVALID_SERVER,
        RESTRICTED_CHANNEL_ADDED,
        LISTING_RESTRICTED_CHANNELS,
        RESTRICTED_CHANNELS_VC_EMBED_FIELD,
        RESTRICTED_CHANNELS_TC_EMBED_FIELD,
        NO_CHANNELS
    }


    enum class ThemeMessages : LocaleMessageKt {
        THEME_SET,
        THEME_EMBED_TITLE,
        THEME_EMBED_DESC,
        THEME_SELECT_MENU_PLACEHOLDER,
        THEME_GREEN,
        THEME_MINT,
        THEME_GOLD,
        THEME_RED,
        THEME_PASTEL_RED,
        THEME_PINK,
        THEME_PURPLE,
        THEME_PASTEL_PURPLE,
        THEME_BLUE,
        THEME_LIGHT_BLUE,
        THEME_BABY_BLUE,
        THEME_ORANGE,
        THEME_YELLOW,
        THEME_PASTEL_YELLOW,
        THEME_DARK,
        THEME_LIGHT
    }


    enum class PremiumMessages : LocaleMessageKt {
        LOCKED_COMMAND_EMBED_TITLE,
        LOCKED_COMMAND_EMBED_DESC,
        NOT_PREMIUM,
        IS_PREMIUM
    }


    enum class TogglesMessages : LocaleMessageKt {
        TOGGLES_EMBED_TITLE,
        TOGGLES_EMBED_TOGGLE_ID_FIELD,
        TOGGLES_MESSAGES_EMBED_FEATURE_FIELD,
        TOGGLES_EMBED_STATUS_FIELD,
        DJ_TOGGLES_EMBED_COMMAND_FIELD,
        DJ_TOGGLES_EMBED_STATUS_FIELD,
        LOG_TOGGLES_EMBED_TYPE_FIELD,
        LOG_TOGGLES_EMBED_STATUS_FIELD,
        TOGGLED,
        INVALID_TOGGLE,
        DJ_TOGGLE_INVALID_COMMAND,
        DJ_TOGGLED,
        LOG_TOGGLE_INVALID_TYPE,
        LOG_TOGGLED,
        SKIP_DJ_TOGGLE_PROMPT
    }


    enum class TwentyFourSevenMessages : LocaleMessageKt {
        TWENTY_FOUR_SEVEN_TOGGLED
    }


    enum class DedicatedChannelMessages : LocaleMessageKt {
        DEDICATED_CHANNEL_SETUP,
        DEDICATED_CHANNEL_SETUP_2,
        DEDICATED_CHANNEL_ALREADY_SETUP,
        DEDICATED_CHANNEL_NOTHING_PLAYING,
        DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING,
        DEDICATED_CHANNEL_QUEUE_PLAYING,
        DEDICATED_CHANNEL_NO_ACCESS_ANYMORE,
        DEDICATED_CHANNEL_PLAYING_EMBED_TITLE,
        DEDICATED_CHANNEL_PLAYING_EMBED_FOOTER,
        DEDICATED_CHANNEL_QUEUE_NO_SONGS,
        DEDICATED_CHANNEL_TOPIC_PREVIOUS,
        DEDICATED_CHANNEL_TOPIC_REWIND,
        DEDICATED_CHANNEL_TOPIC_PLAY_AND_PAUSE,
        DEDICATED_CHANNEL_TOPIC_STOP,
        DEDICATED_CHANNEL_TOPIC_END,
        DEDICATED_CHANNEL_TOPIC_STAR,
        DEDICATED_CHANNEL_TOPIC_LOOP,
        DEDICATED_CHANNEL_TOPIC_SHUFFLE,
        DEDICATED_CHANNEL_TOPIC_QUIT,
        DEDICATED_CHANNEL_EDIT_EMBED,
        DEDICATED_CHANNEL_NOT_SET,
        DEDICATED_CHANNEL_PREVIOUS,
        DEDICATED_CHANNEL_REWIND,
        DEDICATED_CHANNEL_PLAY_AND_PAUSE,
        DEDICATED_CHANNEL_STOP,
        DEDICATED_CHANNEL_SKIP,
        DEDICATED_CHANNEL_FAVOURITE,
        DEDICATED_CHANNEL_LOOP,
        DEDICATED_CHANNEL_SHUFFLE,
        DEDICATED_CHANNEL_DISCONNECT,
        DEDICATED_CHANNEL_FILTERS,
        DEDICATED_CHANNEL_BUTTON_TOGGLE,
        DEDICATED_CHANNEL_NO_CONTENT_INTENT,
        DEDICATED_CHANNEL_SELF_INSUFFICIENT_PERMS_EDIT
    }


    enum class PermissionsMessages : LocaleMessageKt {
        PERMISSIONS_NONE,
        MENTIONABLE_PERMISSIONS_NONE,
        PERMISSIONS_LIST,
        PERMISSION_LIST,
        MENTIONABLE_PERMISSIONS_LIST,
        PERMISSIONS_ROLES,
        PERMISSIONS_USERS,
        PERMISSION_ADDED,
        MENTIONABLE_ALREADY_HAS_PERMISSION,
        PERMISSION_REMOVED,
        MENTIONABLE_NEVER_HAD_PERMISSION,
        DJ_REMOVED,
        DJ_SET,
        NOT_DJ,
        ALREADY_DJ,
        INVALID_PERMISSION
    }


    enum class MentionableTypeMessages : LocaleMessageKt {
        ROLE,
        USER
    }


    enum class PollMessages : LocaleMessageKt {
        POLL_ENDS_AT,
        POLL_BY,
        POLL_SENT,
        POLL_WINNER_LABEL,
        POLL_ENDED
    }


    enum class ReminderMessages : LocaleMessageKt {
        REMINDERS_EMBED_TITLE,
        CANNOT_SET_BANNED_REMINDER_CHANNEL,
        REMINDER_INVALID_TIME_FORMAT,
        REMINDER_ADDED,
        NO_REMINDERS,
        INVALID_REMINDER_ID,
        MISSING_REMINDER_ID,
        REMINDER_REMOVED,
        REMINDERS_CLEARED,
        NO_REMINDER_WITH_ID,
        REMINDER_CHANNEL_REMOVED,
        REMINDER_CHANNEL_CHANGED,
        REMINDER_TIME_CHANGED,
        REMINDER_CHANNEL_ALREADY_BANNED,
        REMINDER_CHANNEL_NOT_BANNED,
        REMINDER_SEND,
        REMINDER_FROM
    }


    enum class EightBallMessages : LocaleMessageKt {
        MUST_PROVIDE_SOMETHING_TO_RESPOND_TO,
        PROVIDE_INDEX_TO_REMOVE,
        INVALID_INDEX_INTEGER,
        MISSING_RESPONSE_TO_ADD,
        ALREADY_A_RESPONSE,
        ADDED_RESPONSE,
        NOT_A_RESPONSE,
        REMOVED_RESPONSE,
        CLEARED_RESPONSES,
        NO_CUSTOM_RESPONSES,
        LIST_OF_RESPONSES,
        QUESTION_ASKED,
        EB_AF_1,
        EB_AF_2,
        EB_AF_3,
        EB_AF_4,
        EB_AF_5,
        EB_AF_6,
        EB_AF_7,
        EB_AF_8,
        EB_AF_9,
        EB_AF_10,
        EB_NC_1,
        EB_NC_2,
        EB_NC_3,
        EB_NC_4,
        EB_NC_5,
        EB_N_1,
        EB_N_2,
        EB_N_3,
        EB_N_4,
        EB_N_5
    }


    enum class PlaytimeMessages : LocaleMessageKt {
        LISTENED_TO,
        LAST_BOOTED
    }


    enum class AlertMessages : LocaleMessageKt {
        ALERT_EMBED_TITLE,
        NO_ALERT,
        ALERT_EMBED_FOOTER
    }


    enum class BotInfoMessages : LocaleMessageKt {
        BOT_INFO_DEVELOPERS,
        BOT_INFO_ABOUT_ME_LABEL,
        BOT_INFO_ABOUT_ME_VALUE,
        BOT_INFO_UPTIME,
        BOT_INFO_TERMS,
        BOT_INFO_PRIVACY
    }


    enum class HelpMessages : LocaleMessageKt {
        HELP_MANAGEMENT_OPTION,
        HELP_MANAGEMENT_OPTION_DESC,
        HELP_MUSIC_OPTION,
        HELP_MUSIC_OPTION_DESC,
        HELP_MISCELLANEOUS_OPTION,
        HELP_MISCELLANEOUS_OPTION_DESC,
        HELP_UTILITY_OPTION,
        HELP_UTILITY_OPTION_DESC,
        HELP_EMBED_AUTHOR,
        HELP_EMBED_DESC,
        HELP_EMBED_FOOTER,
        HELP_NOTHING_FOUND,
        HELP_COMMANDS
    }


    enum class SupportServerMessages : LocaleMessageKt {
        JOIN_SUPPORT_SERVER,
        SUPPORT_SERVER
    }


    enum class AudioLoaderMessages : LocaleMessageKt {
        QUEUE_ADD,
        QUEUE_PLAYLIST_ADD,
        QUEUE_ADD_LOG,
        QUEUE_PLAYLIST_ADD_LOG,
        NO_TRACK_FOUND,
        NO_TRACK_FOUND_ALT,
        ERROR_LOADING_TRACK,
        PLAYING_RECOMMENDED_TRACKS,
        NO_SIMILAR_TRACKS
    }


    enum class AutoPlayMessages : LocaleMessageKt {
        AUTO_PLAY_EMBED_TITLE,
        AUTO_PLAY_EMBED_FOOTER
    }


    enum class TrackSchedulerMessages : LocaleMessageKt {
        TRACK_COULD_NOT_BE_PLAYED,
        COULD_NOT_FIND_SOURCE,
        COPYRIGHT_TRACK,
        UNAVAILABLE_TRACK,
        UNVIEWABLE_PLAYLIST,
        INACTIVITY_LEAVE
    }


    enum class LanguageCommandMessages : LocaleMessageKt {
        LANGUAGE_EMBED_DESC,
        LANGUAGE_SELECT_MENU_PLACE_HOLDER,
        LANGUAGE_CHANGED
    }


    enum class HistoryMessages : LocaleMessageKt {
        NO_PAST_TRACKS,
        HISTORY_EMBED_TITLE
    }

    companion object {
        fun getMessageTypes(): Map<KClass<out LocaleMessageKt>, Array<out LocaleMessageKt>> = mapOf(
            GeneralMessages::class to GeneralMessages.values(),
            RandomMessages::class to RandomMessages.values(),
            FilterMessages::class to FilterMessages.values(),
            ClearQueueMessages::class to ClearQueueMessages.values(),
            DisconnectMessages::class to DisconnectMessages.values(),
            FavouriteTracksMessages::class to FavouriteTracksMessages.values(),
            JoinMessages::class to JoinMessages.values(),
            JumpMessages::class to JumpMessages.values(),
            LofiMessages::class to LofiMessages.values(),
            LoopMessages::class to LoopMessages.values(),
            LyricsMessages::class to LyricsMessages.values(),
            MoveMessages::class to MoveMessages.values(),
            NowPlayingMessages::class to NowPlayingMessages.values(),
            PauseMessages::class to PauseMessages.values(),
            PlayMessages::class to PlayMessages.values(),
            PreviousTrackMessages::class to PreviousTrackMessages.values(),
            QueueMessages::class to QueueMessages.values(),
            RemoveMessages::class to RemoveMessages.values(),
            RewindMessages::class to RewindMessages.values(),
            SearchMessages::class to SearchMessages.values(),
            SeekMessages::class to SeekMessages.values(),
            ShufflePlayMessages::class to ShufflePlayMessages.values(),
            ShuffleMessages::class to ShuffleMessages.values(),
            SkipMessages::class to SkipMessages.values(),
            StopMessages::class to StopMessages.values(),
            VolumeMessages::class to VolumeMessages.values(),
            BanMessages::class to BanMessages.values(),
            UnbanMessages::class to UnbanMessages.values(),
            LogChannelMessages::class to LogChannelMessages.values(),
            RestrictedChannelMessages::class to RestrictedChannelMessages.values(),
            ThemeMessages::class to ThemeMessages.values(),
            PremiumMessages::class to PremiumMessages.values(),
            TogglesMessages::class to TogglesMessages.values(),
            TwentyFourSevenMessages::class to TwentyFourSevenMessages.values(),
            DedicatedChannelMessages::class to DedicatedChannelMessages.values(),
            PermissionsMessages::class to PermissionsMessages.values(),
            MentionableTypeMessages::class to MentionableTypeMessages.values(),
            PollMessages::class to PollMessages.values(),
            ReminderMessages::class to ReminderMessages.values(),
            EightBallMessages::class to EightBallMessages.values(),
            PlaytimeMessages::class to PlaytimeMessages.values(),
            AlertMessages::class to AlertMessages.values(),
            BotInfoMessages::class to BotInfoMessages.values(),
            HelpMessages::class to HelpMessages.values(),
            AudioLoaderMessages::class to AudioLoaderMessages.values(),
            AutoPlayMessages::class to AutoPlayMessages.values(),
            TrackSchedulerMessages::class to TrackSchedulerMessages.values(),
            LanguageCommandMessages::class to LanguageCommandMessages.values(),
            HistoryMessages::class to HistoryMessages.values(),
            SupportServerMessages::class to SupportServerMessages.values()
        )
    }
}