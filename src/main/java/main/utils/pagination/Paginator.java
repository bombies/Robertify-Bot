package main.utils.pagination;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Emoji;

public class Paginator {
    @Getter
    final String front, previous, next, end;
    @Getter
    final Emoji frontEmoji, previousEmoji, nextEmoji, endEmoji;

    public Paginator(String front, String previous, String next, String end) {
        this.front = front;
        this.previous = previous;
        this.next = next;
        this.end = end;
        frontEmoji = null;
        previousEmoji = null;
        nextEmoji = null;
        endEmoji = null;
    }

    public Paginator(Emoji frontEmoji, Emoji previousEmoji, Emoji nextEmoji, Emoji endEmoji) {
        this.frontEmoji = frontEmoji;
        this.previousEmoji = previousEmoji;
        this.nextEmoji = nextEmoji;
        this.endEmoji = endEmoji;
        front = null;
        previous = null;
        next = null;
        end = null;
    }
}
