package main.utils.json.logs

import main.constants.RobertifyEmojiKt
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.awt.Color

enum class LogTypeKt(title: String, emoji: Emoji, color: Color, name: String) {
    QUEUE_ADD("Queue Addition", Emoji.fromUnicode("➕"), Color.decode("#35fc03"), "Queue Additions"),
    QUEUE_REMOVE("Queue Removal", Emoji.fromUnicode("➖"), Color.decode("#ff5e5e"), "Queue Removes"),
    QUEUE_CLEAR("Queue Cleared", Emoji.fromUnicode("🗑️"), Color.decode("#00c497"), "Queue Clears"),
    QUEUE_SHUFFLE("Queue Shuffled", RobertifyEmojiKt.SHUFFLE_EMOJI.getEmoji(), Color.decode("#6900c4"), "Queue Shuffles"),
    QUEUE_LOOP("Queue Loop", RobertifyEmojiKt.LOOP_EMOJI.getEmoji(), Color.decode("#c4007c"), "Queue Loops"),
    PLAYER_STOP("Player Stopped", RobertifyEmojiKt.STOP_EMOJI.getEmoji(), Color.decode("#e3d022"), "Player Stops"),
    PLAYER_PAUSE("Player Paused", RobertifyEmojiKt.PAUSE_EMOJI.getEmoji(), Color.decode("#e38f22"), "Play Pauses"),
    PLAYER_RESUME("Player Resumed", RobertifyEmojiKt.PLAY_EMOJI.getEmoji(), Color.decode("#e36622"), "Player Resume"),
    TRACK_SKIP("Track Skipped", RobertifyEmojiKt.END_EMOJI.getEmoji(), Color.decode("#4f82e0"), "Track Skips"),
    TRACK_VOTE_SKIP("Vote Skip Started", RobertifyEmojiKt.END_EMOJI.getEmoji(), Color.decode("#4f82e0"), "Track Vote Skips"),
    TRACK_REWIND("Track Rewound", RobertifyEmojiKt.REWIND_EMOJI.getEmoji(), Color.decode("#e04f9f"), "Track Rewinds"),
    TRACK_JUMP("Track Jumped", RobertifyEmojiKt.END_EMOJI.getEmoji(), Color.decode("#058f00"), "Track Jumps"),
    TRACK_SEEK("Track Seeked", RobertifyEmojiKt.END_EMOJI.getEmoji(), Color.decode("#ffff00"), "Track Seeks"),
    TRACK_PREVIOUS("Previous Track", RobertifyEmojiKt.PREVIOUS_EMOJI.getEmoji(), Color.decode("#4f66f7"), "Previous Tracks"),
    TRACK_LOOP("Track Loop", RobertifyEmojiKt.LOOP_EMOJI.getEmoji(), Color.decode("#a6f74f"), "Track Loops"),
    TRACK_MOVE("Track Moved", Emoji.fromUnicode("👣"), Color.decode("#a94ff7"), "Track Moves"),
    VOLUME_CHANGE("Volume Changed", Emoji.fromUnicode("🔊"), Color.decode("#004770"), "Volume Changes"),
    BOT_DISCONNECTED("Disconnect", RobertifyEmojiKt.QUIT_EMOJI.getEmoji(), Color.decode("#700000"), "Disconnects"),
    FILTER_TOGGLE("Filter Toggle", Emoji.fromUnicode("🎛️"), Color.decode("#077000"), "Filters");

    companion object {
        fun toList(): List<String> = LogTypeKt.values().map { it.name }.toList()
    }
}