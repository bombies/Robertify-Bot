package main.utils.json.requestchannel

import main.commands.slashcommands.management.requestchannel.RequestChannelButtonId
import main.constants.RobertifyEmojiKt
import net.dv8tion.jda.api.entities.emoji.Emoji

enum class RequestChannelButtonKt(val id: RequestChannelButtonId, val emoji: Emoji) {
    PREVIOUS(RequestChannelButtonId.PREVIOUS, Emoji.fromFormatted(RobertifyEmojiKt.PREVIOUS_EMOJI.toString())),
    REWIND(RequestChannelButtonId.REWIND, Emoji.fromFormatted(RobertifyEmojiKt.REWIND_EMOJI.toString())),
    PLAY_PAUSE(RequestChannelButtonId.PLAY_AND_PAUSE, Emoji.fromFormatted(RobertifyEmojiKt.PLAY_AND_PAUSE_EMOJI.toString())),
    STOP(RequestChannelButtonId.STOP, Emoji.fromFormatted(RobertifyEmojiKt.STOP_EMOJI.toString())),
    SKIP(RequestChannelButtonId.SKIP, Emoji.fromFormatted(RobertifyEmojiKt.END_EMOJI.toString())),
    FAVOURITE(RequestChannelButtonId.FAVOURITE, Emoji.fromFormatted(RobertifyEmojiKt.STAR_EMOJI.toString())),
    LOOP(RequestChannelButtonId.LOOP, Emoji.fromFormatted(RobertifyEmojiKt.LOOP_EMOJI.toString())),
    SHUFFLE(RequestChannelButtonId.SHUFFLE, Emoji.fromFormatted(RobertifyEmojiKt.SHUFFLE_EMOJI.toString())),
    DISCONNECT(RequestChannelButtonId.DISCONNECT, Emoji.fromFormatted(RobertifyEmojiKt.QUIT_EMOJI.toString())),
    FILTERS(RequestChannelButtonId.FILTERS, Emoji.fromFormatted(RobertifyEmojiKt.FILTER_EMOJI.toString()));

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