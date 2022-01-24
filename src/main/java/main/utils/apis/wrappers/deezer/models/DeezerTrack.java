package main.utils.apis.wrappers.deezer.models;

import lombok.Getter;

import java.net.URL;
import java.util.Date;

public class DeezerTrack {
    @Getter
    private final int id;
    @Getter
    private final boolean readable;
    @Getter
    private final String title;
    @Getter
    private final String shortTitle;
    @Getter
    private final String version;
    @Getter
    private final boolean unseen;
    @Getter
    private final String isrc;
    @Getter
    private final URL link;
    @Getter
    private final URL share;
    @Getter
    private final int duration;
    @Getter
    private final int trackPosition;
    @Getter
    private final int diskNumber;
    @Getter
    private final int rank;
    @Getter
    private final Date releaseDate;
    @Getter
    private final boolean explicitLyrics;
    @Getter
    private final int explicitCover;
    @Getter
    private final URL preview;
    @Getter
    private final float bpm;
    @Getter
    private final float gain;
    // TODO Add album and artists


    protected DeezerTrack(int id, boolean readable, String title,
                       String shortTitle, String version, boolean unseen,
                       String isrc, URL link, URL share, int duration,
                       int trackPosition, int diskNumber, int rank,
                       Date releaseDate, boolean explicitLyrics, int explicitCover,
                       URL preview, float bpm, float gain) {
        this.id = id;
        this.readable = readable;
        this.title = title;
        this.shortTitle = shortTitle;
        this.version = version;
        this.unseen = unseen;
        this.isrc = isrc;
        this.link = link;
        this.share = share;
        this.duration = duration;
        this.trackPosition = trackPosition;
        this.diskNumber = diskNumber;
        this.rank = rank;
        this.releaseDate = releaseDate;
        this.explicitLyrics = explicitLyrics;
        this.explicitCover = explicitCover;
        this.preview = preview;
        this.bpm = bpm;
        this.gain = gain;
    }
}
