package main.utils.apis.wrappers.deezer.models;

import java.net.URL;

public class DeezerArtist {

    private final int id;
    private final String name;
    private final URL link;
    private final URL share;
    private final URL picture;
    private final URL smallPicture;
    private final URL mediumPicture;
    private final URL bigPicture;
    private final URL XLPicture;
    private final int numberOfAlbums;
    private final int numberOfFans;
    private final boolean hasRadio;
    private final URL tracklist;

    public DeezerArtist(int id, String name, URL link, URL share,
                        URL picture, URL smallPicture, URL mediumPicture,
                        URL bigPicture, URL XLPicture, int numberOfAlbums,
                        int numberOfFans, boolean hasRadio, URL tracklist) {
        this.id = id;
        this.name = name;
        this.link = link;
        this.share = share;
        this.picture = picture;
        this.smallPicture = smallPicture;
        this.mediumPicture = mediumPicture;
        this.bigPicture = bigPicture;
        this.XLPicture = XLPicture;
        this.numberOfAlbums = numberOfAlbums;
        this.numberOfFans = numberOfFans;
        this.hasRadio = hasRadio;
        this.tracklist = tracklist;
    }
}
