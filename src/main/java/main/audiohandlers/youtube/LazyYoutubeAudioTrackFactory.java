package main.audiohandlers.youtube;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.AudioTrackFactory;
import main.audiohandlers.TrackMeta;

import java.util.List;
import java.util.stream.Collectors;

public class LazyYoutubeAudioTrackFactory implements AudioTrackFactory {
    private final YoutubeSearchProvider ytSearchProvider;
    private final YoutubeAudioSourceManager ytAudioSourceManager;

    @Inject
    public LazyYoutubeAudioTrackFactory(YoutubeSearchProvider ytSearchProvider,
                                        YoutubeAudioSourceManager ytAudioSourceManager) {
        this.ytSearchProvider = Preconditions.checkNotNull(ytSearchProvider, "ytSearchProvider must be non-null.");
        this.ytAudioSourceManager = Preconditions.checkNotNull(ytAudioSourceManager,
                "ytAudioSourceManager must be non-null.");
    }

    @Override
    public List<AudioTrack> getAudioTrack(List<TrackMeta> trackMeta) {
        Preconditions.checkNotNull(trackMeta, "songMetadata must be non-null.");

        return trackMeta.stream().map(sm -> getAudioTrack(sm)).collect(Collectors.toList());
    }

    @Override
    public AudioTrack getAudioTrack(TrackMeta trackMeta) {
        Preconditions.checkNotNull(trackMeta, "songMetadata must be non-null.");

        AudioTrackInfo ati = new AudioTrackInfo(trackMeta.getName(), trackMeta.getArtist(),
                trackMeta.getDuration(), "", false, "");
        return new LazyYoutubeAudioTrack(ati, ytAudioSourceManager, ytSearchProvider);
    }
}
