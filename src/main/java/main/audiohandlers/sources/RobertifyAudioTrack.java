package main.audiohandlers.sources;

import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import main.audiohandlers.sources.deezer.DeezerSourceManager;
import main.audiohandlers.sources.spotify.SpotifySourceManager;
import main.audiohandlers.sources.spotify.SpotifyTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RobertifyAudioTrack extends DelegatedAudioTrack {
    private static final Logger logger = LoggerFactory.getLogger(RobertifyAudioTrack.class);

    public final String isrc;
    public final String artworkURL;
    private final String spotifyArtistID;
    private final List<String> spotifyArtistGenres;
    private final RobertifyAudioSourceManager audioSourceManager;

    public String getIsrc() {
        return this.isrc;
    }

    public String getArtworkURL() {
        return this.artworkURL;
    }

    public RobertifyAudioSourceManager getAudioSourceManager() {
        return this.audioSourceManager;
    }

    public RobertifyAudioTrack(String title, String identifier, String isrc, String image, String artistName, Integer duration, RobertifyAudioSourceManager audioSourceManager) {
        this(new AudioTrackInfo(title, artistName, duration

                .longValue(), identifier, false, audioSourceManager instanceof SpotifySourceManager ? "https://open.spotify.com/track/" + identifier : audioSourceManager instanceof DeezerSourceManager ? "https://deezer.com/us/track/" + identifier : identifier), isrc, image, audioSourceManager);
    }

    public RobertifyAudioTrack(AudioTrackInfo info, String isrc, String artworkURL, RobertifyAudioSourceManager audioSourceManager) {
        super(info);
        this.isrc = isrc;
        this.artworkURL = artworkURL;
        this.audioSourceManager = audioSourceManager;
        this.spotifyArtistID = null;
        this.spotifyArtistGenres = null;
    }

    public RobertifyAudioTrack(AudioTrackInfo info, String isrc, String artworkURL,
                               String spotifyArtistID, List<String> spotifyArtistGenres,
                               RobertifyAudioSourceManager audioSourceManager) {
        super(info);
        this.isrc = isrc;
        this.artworkURL = artworkURL;
        this.audioSourceManager = audioSourceManager;
        this.spotifyArtistID = spotifyArtistID;
        this.spotifyArtistGenres = spotifyArtistGenres;
    }

    public RobertifyAudioTrack(AudioTrackInfo info, String isrc, String artworkURL,
                               String artist,
                               RobertifyAudioSourceManager audioSourceManager) {
        super(info);
        this.isrc = isrc;
        this.artworkURL = artworkURL;
        this.audioSourceManager = audioSourceManager;

        final var spotifyArtist = SpotifyTrack.assembleArtist(artist);
        this.spotifyArtistID = spotifyArtist.getId();
        this.spotifyArtistGenres = spotifyArtist.getGenres();
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        // ;)
    }
}
