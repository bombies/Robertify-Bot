package main.audiohandlers.spotify;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

public class SpotifyAudioTrack extends DelegatedAudioTrack {
    private final YoutubeAudioSourceManager manager;

    public SpotifyAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager manager) {
        super(trackInfo);
        this.manager = manager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        AudioItem item = manager.loadItem(null, new AudioReference(trackInfo.identifier, null));
        if (item instanceof AudioPlaylist)
            ((YoutubeAudioTrack) ((AudioPlaylist) item).getTracks().get(0)).process(executor);
    }
}
