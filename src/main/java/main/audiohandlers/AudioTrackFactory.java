package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public interface AudioTrackFactory {
    List<AudioTrack> getAudioTrack(List<TrackMeta> trackMeta);

    AudioTrack getAudioTrack(TrackMeta trackMeta);
}
