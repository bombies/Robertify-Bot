package main.audiohandlers.sources;

import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
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

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        // ;)
    }
}
