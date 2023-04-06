package main.utils.pagination.pages.queue;

import lombok.Getter;

import javax.annotation.Nullable;

public class QueueItem {
    @Getter
    private final int trackIndex;
    @Getter
    private final String trackTitle;
    @Getter
    private final String artist;
    @Getter
    private final long duration;

    public QueueItem(int trackIndex, String trackTitle, String artist, long duration) {
        this.trackIndex = trackIndex;
        this.trackTitle = trackTitle;
        this.artist = artist;
        this.duration = duration;
    }
}
