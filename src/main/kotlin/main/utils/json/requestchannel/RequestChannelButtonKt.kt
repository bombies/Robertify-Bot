package main.utils.json.requestchannel

import main.constants.RobertifyEmojiKt
import net.dv8tion.jda.api.entities.emoji.Emoji

enum class RequestChannelButtonKt(val id: String, val emoji: Emoji) {
    PREVIOUS("dedicatedprevious", Emoji.fromFormatted(RobertifyEmojiKt.PREVIOUS_EMOJI.toString())),
    REWIND("dedicatedrewind", Emoji.fromFormatted(RobertifyEmojiKt.REWIND_EMOJI.toString())),
    PLAY_PAUSE("dedicatedpnp", Emoji.fromFormatted(RobertifyEmojiKt.PLAY_AND_PAUSE_EMOJI.toString())),
    STOP("dedicatedstop", Emoji.fromFormatted(RobertifyEmojiKt.STOP_EMOJI.toString())),
    SKIP("dedicatedskip", Emoji.fromFormatted(RobertifyEmojiKt.END_EMOJI.toString())),
    FAVOURITE("dedicatedfavourite", Emoji.fromFormatted(RobertifyEmojiKt.STAR_EMOJI.toString())),
    LOOP("dedicatedloop", Emoji.fromFormatted(RobertifyEmojiKt.LOOP_EMOJI.toString())),
    SHUFFLE("dedicatedshuffle", Emoji.fromFormatted(RobertifyEmojiKt.SHUFFLE_EMOJI.toString())),
    DISCONNECT("dedicateddisconnect", Emoji.fromFormatted(RobertifyEmojiKt.QUIT_EMOJI.toString())),
    FILTERS("dedicatedfilters", Emoji.fromFormatted(RobertifyEmojiKt.FILTER_EMOJI.toString()));

    companion object {
        val firstRow: List<RequestChannelButtonKt>
            get() = listOf(
                PREVIOUS,
                REWIND,
                PLAY_PAUSE,
                STOP,
                SKIP
            )
        val secondRow: List<RequestChannelButtonKt>
            get() = listOf(
                FAVOURITE,
                LOOP,
                SHUFFLE,
                DISCONNECT
            )
        val finalRow: List<RequestChannelButtonKt>
            get() = listOf(FILTERS)
    }
}