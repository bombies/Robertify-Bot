package main.utils.json.logs;

import lombok.Getter;
import main.constants.RobertifyEmoji;
import net.dv8tion.jda.api.entities.Emoji;

import java.awt.*;

public enum LogType {
    QUEUE_ADD("Queue Addition", Emoji.fromUnicode("➕"), Color.decode("#35fc03")),
    QUEUE_REMOVE("Queue Removal", Emoji.fromUnicode("➖"), Color.decode("#ff5e5e")),
    QUEUE_CLEAR("Queue Cleared", Emoji.fromUnicode("🗑️"), Color.decode("#00c497")),
    QUEUE_SHUFFLE("Queue Shuffled", RobertifyEmoji.SHUFFLE_EMOJI.getEmoji(), Color.decode("#6900c4")),
    QUEUE_LOOP("Queue Loop", RobertifyEmoji.LOOP_EMOJI.getEmoji(), Color.decode("#c4007c")),
    PLAYER_STOP("Player Stopped", RobertifyEmoji.STOP_EMOJI.getEmoji(), Color.decode("#e3d022")),
    PLAYER_PAUSE("Player Paused", RobertifyEmoji.PAUSE_EMOJI.getEmoji(), Color.decode("#e38f22")),
    PLAYER_RESUME("Player Resumed", RobertifyEmoji.PLAY_EMOJI.getEmoji(), Color.decode("#e36622")),
    TRACK_SKIP("Track Skipped", RobertifyEmoji.END_EMOJI.getEmoji(), Color.decode("#4f82e0")),
    TRACK_VOTE_SKIP("Vote Skip Started", RobertifyEmoji.END_EMOJI.getEmoji(), Color.decode("#4f82e0")),
    TRACK_REWIND("Track Rewound", RobertifyEmoji.REWIND_EMOJI.getEmoji(), Color.decode("#e04f9f")),
    TRACK_JUMP("Track Jumped", RobertifyEmoji.END_EMOJI.getEmoji(), Color.decode("#058f00")),
    TRACK_SEEK("Track Seeked", RobertifyEmoji.END_EMOJI.getEmoji(), Color.decode("#ffff00")),
    TRACK_PREVIOUS("Previous Track", RobertifyEmoji.PREVIOUS_EMOJI.getEmoji(), Color.decode("#4f66f7")),
    TRACK_LOOP("Track Loop", RobertifyEmoji.LOOP_EMOJI.getEmoji(), Color.decode("#a6f74f")),
    TRACK_MOVE("Track Moved", Emoji.fromUnicode("👣"), Color.decode("#a94ff7")),
    VOLUME_CHANGE("Volume Changed", Emoji.fromUnicode("🔊"), Color.decode("#004770")),
    BOT_DISCONNECTED("Disconnect", RobertifyEmoji.QUIT_EMOJI.getEmoji(), Color.decode("#700000")),
    FILTER_TOGGLE("Filter Toggle", Emoji.fromUnicode("🎛️"), Color.decode("#077000"));

    @Getter
    private final String title;
    @Getter
    private final Emoji emoji;
    @Getter
    private final Color color;

    LogType(String title, Emoji emoji, Color color) {
        this.title = title;
        this.emoji = emoji;
        this.color = color;
    }
}
