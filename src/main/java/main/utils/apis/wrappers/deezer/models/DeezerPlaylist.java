package main.utils.apis.wrappers.deezer.models;

import lombok.Getter;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.net.URL;

public class DeezerPlaylist {
    @Getter
    private final int id;
    @Getter
    private final String title;
    @Getter
    private final String description;
    @Getter
    private final int duration;
    @Getter
    private final boolean isPublic;
    @Getter
    private final boolean lovedTrackPlaylist;
    @Getter
    private final boolean collaborative;
    @Getter
    private final int numberOfTracks;
    @Getter
    private final int unseenTracks;
    @Getter
    private final int fans;
    @Getter
    private final URL link;
    @Getter
    private final URL share;
    @Getter
    private final URL picture;
    @Getter
    private final URL smallPicture;
    @Getter
    private final URL mediumPicture;
    @Getter
    private final URL bigPicture;
    @Getter
    private final URL XLPicture;
    @Getter
    private final Pair<Integer, String> creator;

    public DeezerPlaylist(int id, String title, String description, int duration,
                          boolean isPublic, boolean lovedTrackPlaylist, boolean collaborative,
                          int numberOfTracks, int unseenTracks, int fans,
                          URL link, URL share, URL picture,
                          URL smallPicture, URL mediumPicture, URL bigPicture,
                          URL XLPicture, Pair<Integer, String> creator) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.isPublic = isPublic;
        this.lovedTrackPlaylist = lovedTrackPlaylist;
        this.collaborative = collaborative;
        this.numberOfTracks = numberOfTracks;
        this.unseenTracks = unseenTracks;
        this.fans = fans;
        this.link = link;
        this.share = share;
        this.picture = picture;
        this.smallPicture = smallPicture;
        this.mediumPicture = mediumPicture;
        this.bigPicture = bigPicture;
        this.XLPicture = XLPicture;
        this.creator = creator;
    }
}
