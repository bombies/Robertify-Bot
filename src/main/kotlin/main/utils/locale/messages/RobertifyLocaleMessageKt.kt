package main.utils.locale.messages

import main.utils.locale.LocaleMessageKt
import kotlin.reflect.KClass

class RobertifyLocaleMessageKt {

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
            SupportServerMessages::class to SupportServerMessages.values(),
            DuplicateMessages::class to DuplicateMessages.values()
        )
    }
}