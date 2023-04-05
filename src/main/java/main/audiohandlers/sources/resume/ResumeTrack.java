package main.audiohandlers.sources.resume;

import com.github.topisenpai.lavasrc.mirror.MirroringAudioSourceManager;
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class ResumeTrack extends MirroringAudioTrack {

    public ResumeTrack(AudioTrackInfo trackInfo, String isrc, String artworkURL, MirroringAudioSourceManager sourceManager) {
        super(trackInfo, isrc, artworkURL, sourceManager);
    }
}

