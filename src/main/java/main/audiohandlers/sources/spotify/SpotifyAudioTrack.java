package main.audiohandlers.sources.spotify;

import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import main.audiohandlers.sources.RobertifyAudioTrack;

public class SpotifyAudioTrack extends RobertifyAudioTrack {

    public SpotifyAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager youtubeAudioSourceManager, SoundCloudAudioSourceManager soundCloudAudioSourceManager,
                             String spotifyID, String trackImage, SpotifyAudioSourceManager sourceManager) {
        super(trackInfo, youtubeAudioSourceManager, soundCloudAudioSourceManager, spotifyID, trackImage, sourceManager);
    }
}
