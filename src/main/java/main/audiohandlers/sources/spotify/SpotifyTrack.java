package main.audiohandlers.sources.spotify;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.sources.RobertifyAudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.*;

public class SpotifyTrack extends RobertifyAudioTrack {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyTrack.class);

    private final SpotifySourceManager spotifySourceManager;

    public SpotifyTrack(String title, String identifier, String isrc, Image[] images, String uri, ArtistSimplified[] artists, Integer trackDuration, SpotifySourceManager spotifySourceManager) {
        this(new AudioTrackInfo(title,
                        (artists.length == 0) ? "unknown" : artists[0].getName(), trackDuration
                        .longValue(), identifier, false, "https://open.spotify.com/track/" + identifier), isrc,

                (images.length == 0) ? null : images[0].getUrl(), spotifySourceManager);
    }

    public SpotifyTrack(AudioTrackInfo trackInfo, String isrc, String artworkURL, SpotifySourceManager spotifySourceManager) {
        super(trackInfo, isrc, artworkURL, spotifySourceManager);
        this.spotifySourceManager = spotifySourceManager;
    }

    public static SpotifyTrack of(TrackSimplified track, Album album, SpotifySourceManager spotifySourceManager) {
        return new SpotifyTrack(track.getName(), track.getId(), null, album.getImages(), track.getUri(), track.getArtists(), track.getDurationMs(), spotifySourceManager);
    }

    public static SpotifyTrack of(Track track, SpotifySourceManager spotifySourceManager) {
        return new SpotifyTrack(track.getName(), track.getId(), track.getExternalIds().getExternalIds().getOrDefault("isrc", null), track.getAlbum().getImages(), track.getUri(), track.getArtists(), track.getDurationMs(), spotifySourceManager);
    }

    public String getISRC() {
        return this.isrc;
    }

    public String getArtworkURL() {
        return this.artworkURL;
    }

    public AudioSourceManager getSourceManager() {
        return (AudioSourceManager)this.spotifySourceManager;
    }

    protected AudioTrack makeShallowClone() {
        return new SpotifyTrack(getInfo(), this.isrc, this.artworkURL, this.spotifySourceManager);
    }
}
