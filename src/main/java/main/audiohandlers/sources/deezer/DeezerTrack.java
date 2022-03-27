package main.audiohandlers.sources.deezer;

import api.deezer.objects.Artist;
import api.deezer.objects.Track;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.sources.RobertifyAudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeezerTrack extends RobertifyAudioTrack {
    private static final Logger logger = LoggerFactory.getLogger(DeezerTrack.class);

    private final DeezerSourceManager deezerSourceManager;

    public DeezerTrack(String title, String identifier, String isrc, String image, String uri, Artist artist, Integer duration, DeezerSourceManager deezerSourceManager) {
        this(new AudioTrackInfo(title, artist
                .getName(), duration
                .longValue(), identifier, false, "https://open.spotify.com/track/" + identifier), isrc, image, deezerSourceManager);
    }

    public DeezerTrack(AudioTrackInfo info, String isrc, String artworkURL, DeezerSourceManager deezerSourceManager) {
        super(info, isrc, artworkURL, deezerSourceManager);
        this.deezerSourceManager = deezerSourceManager;
    }

    public static DeezerTrack of(Track track, DeezerSourceManager deezerSourceManager) {
        return new DeezerTrack(track.getTitle(), String.valueOf(track.getId()), track.getIsrc(), track.getAlbum().getCoverXl(), track.getLink(), track.getArtist(), track.getDuration(), deezerSourceManager);
    }

    public AudioSourceManager getSourceManager() {
        return (AudioSourceManager)this.deezerSourceManager;
    }

    protected AudioTrack makeShallowClone() {
        return (AudioTrack)new DeezerTrack(getInfo(), this.isrc, this.artworkURL, this.deezerSourceManager);
    }
}
