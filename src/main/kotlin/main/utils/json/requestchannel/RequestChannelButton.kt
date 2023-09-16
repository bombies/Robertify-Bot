package main.utils.json.requestchannel

import main.constants.RobertifyEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji

enum class RequestChannelButton(val id: RequestChannelButtonId, val emoji: Emoji) {
    PREVIOUS(RequestChannelButtonId.PREVIOUS, Emoji.fromFormatted(RobertifyEmoji.PREVIOUS_EMOJI.toString())),
    REWIND(RequestChannelButtonId.REWIND, Emoji.fromFormatted(RobertifyEmoji.REWIND_EMOJI.toString())),
    PLAY_PAUSE(RequestChannelButtonId.PLAY_AND_PAUSE, Emoji.fromFormatted(RobertifyEmoji.PLAY_AND_PAUSE_EMOJI.toString())),
    STOP(RequestChannelButtonId.STOP, Emoji.fromFormatted(RobertifyEmoji.STOP_EMOJI.toString())),
    SKIP(RequestChannelButtonId.SKIP, Emoji.fromFormatted(RobertifyEmoji.END_EMOJI.toString())),
    FAVOURITE(RequestChannelButtonId.FAVOURITE, Emoji.fromFormatted(RobertifyEmoji.STAR_EMOJI.toString())),
    LOOP(RequestChannelButtonId.LOOP, Emoji.fromFormatted(RobertifyEmoji.LOOP_EMOJI.toString())),
    SHUFFLE(RequestChannelButtonId.SHUFFLE, Emoji.fromFormatted(RobertifyEmoji.SHUFFLE_EMOJI.toString())),
    DISCONNECT(RequestChannelButtonId.DISCONNECT, Emoji.fromFormatted(RobertifyEmoji.QUIT_EMOJI.toString())),
    FILTERS(RequestChannelButtonId.FILTERS, Emoji.fromFormatted(RobertifyEmoji.FILTER_EMOJI.toString()));

    companion object {
        val firstRow: List<RequestChannelButton>
            get() = listOf(
                PREVIOUS,
                REWIND,
                PLAY_PAUSE,
                STOP,
                SKIP
            )
        val secondRow: List<RequestChannelButton>
            get() = listOf(
                FAVOURITE,
                LOOP,
                SHUFFLE,
                DISCONNECT
            )
        val finalRow: List<RequestChannelButton>
            get() = listOf(FILTERS)
    }
}