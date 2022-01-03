package main.audiohandlers.sources.deezer;

import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.sources.RobertifyAudioTrack;

public class DeezerAudioTrack extends RobertifyAudioTrack {

    public DeezerAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager youtubeAudioSourceManager, SoundCloudAudioSourceManager soundCloudAudioSourceManager,
                            Integer deezerID, String trackImage) {
        super(trackInfo, youtubeAudioSourceManager, soundCloudAudioSourceManager, String.valueOf(deezerID), trackImage);
    }
}
