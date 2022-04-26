package main.audiohandlers.sources;

import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import main.audiohandlers.sources.deezer.DeezerSourceManager;
import main.audiohandlers.sources.spotify.SpotifySourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobertifyAudioTrack extends DelegatedAudioTrack {
    private static final Logger logger = LoggerFactory.getLogger(RobertifyAudioTrack.class);

    public final String isrc;
    public final String artworkURL;
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
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        // ;)
    }
}
