package main.constants

import main.utils.json.GenericJSONFieldKt

enum class ToggleKt(private val str: String) : GenericJSONFieldKt {
    RESTRICTED_VOICE_CHANNELS("restricted_voice_channels"),
    RESTRICTED_TEXT_CHANNELS("restricted_text_channels"),
    ANNOUNCE_MESSAGES("announce_messages"),
    SHOW_REQUESTER("show_requester"),
    EIGHT_BALL("8ball"),
    POLLS("polls"),
    REMINDERS("reminders"),
    TIPS("tips"),
    VOTE_SKIPS("vote_skips");

    companion object {
        fun parseToggle(toggle: ToggleKt): String = when (toggle) {
            ANNOUNCE_MESSAGES -> "announcements"
            SHOW_REQUESTER -> "requester"
            EIGHT_BALL -> "8ball"
            POLLS -> "polls"
            REMINDERS -> "reminders"
            RESTRICTED_VOICE_CHANNELS -> "restrictedvoice"
            RESTRICTED_TEXT_CHANNELS -> "restrictedtext"
            TIPS -> "tips"
            VOTE_SKIPS ->"voteskips"
        }

        fun toList(): List<String> = ToggleKt.values()
            .map { parseToggle(it) }
    }

    override fun toString(): String = str

    enum class TogglesConfigField(private val str: String) : GenericJSONFieldKt {
        DJ_TOGGLES("dj_toggles"),
        LOG_TOGGLES("log_toggles");

        override fun toString(): String = str
    }

}