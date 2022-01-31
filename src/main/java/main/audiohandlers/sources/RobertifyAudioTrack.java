package main.audiohandlers.sources;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import lombok.Getter;
import main.audiohandlers.RobertifyAudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RobertifyAudioTrack extends DelegatedAudioTrack {
    private final Logger logger = LoggerFactory.getLogger(RobertifyAudioTrack.class);

    private final YoutubeAudioSourceManager youtubeAudioSourceManager;
    private final SoundCloudAudioSourceManager soundCloudAudioSourceManager;
    private final AudioSourceManager sourceManager;
    @Getter
    private final String id;
    @Getter
    private final String trackImage;

    public RobertifyAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager youtubeAudioSourceManager, SoundCloudAudioSourceManager soundCloudAudioSourceManager,
                               String id, String trackImage, AudioSourceManager sourceManager) {
        super(trackInfo);
        logger.info("Constructing track...\nID: {}\nTitle: {}\nAuthor: {}\nLength: {}\nIs Stream: {}\nuri: {}",
                trackInfo.identifier, trackInfo.title, trackInfo.author, trackInfo.length, trackInfo.isStream, trackInfo.uri);
        this.youtubeAudioSourceManager = youtubeAudioSourceManager;
        this.soundCloudAudioSourceManager = soundCloudAudioSourceManager;
        this.id = id;
        this.trackImage = trackImage;
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        logger.info("Processing...");

        var item = loadItem("ytmsearch:" + trackInfo.title + " by " + trackInfo.author);

        if (item instanceof AudioPlaylist playlist) {
            logger.info("[FROM SOURCE] {} - {} [{}]", trackInfo.title.split("[(\\-]")[0].strip(), trackInfo.author, trackInfo.length);
            logger.info("Searching YouTube Music...");
            AudioTrack track = search(playlist);

            if (track == null) {
                item = loadItem("ytsearch:" + trackInfo.title + " by " + trackInfo.author);

                if (item instanceof AudioPlaylist newPlaylist) {
                    logger.info("Searching YouTube...");
                    track = search(newPlaylist);
                }
            }

            if (track == null) {
                item = loadItem("scsearch:" + trackInfo.title + " by " + trackInfo.author);

                if (item instanceof AudioPlaylist newPlaylist) {
                    logger.info("Searching SoundCloud...");
                    track = search(newPlaylist);
                }
            }

//            if (track instanceof YoutubeAudioTrack ytTrack)
//                ytTrack.process(executor);
//            else if (track instanceof SoundCloudAudioTrack scTrack)
//                scTrack.process(executor);

            if (track instanceof InternalAudioTrack iTrack) {
                processDelegate(iTrack, executor);
                return;
            }

            throw new FriendlyException("No track found", FriendlyException.Severity.COMMON, new NullPointerException());
        }
    }

    private AudioTrack search(AudioPlaylist playlist) {
        for (AudioTrack audioTrack : playlist.getTracks()) {

            logger.info("{} - {} [{}]", audioTrack.getInfo().title, audioTrack.getInfo().author, audioTrack.getDuration());

            if (audioTrack.getDuration() >= trackInfo.length - 7000
                    && audioTrack.getDuration() <= trackInfo.length + 5000
                    && (audioTrack.getInfo().title.toLowerCase().contains(trackInfo.title.toLowerCase().split("[(\\-]")[0].strip()) ||
                    audioTrack.getInfo().title.toLowerCase().contains(trackInfo.title.toLowerCase()))
            ) {
                if (audioTrack.getInfo().title.contains("clean")
                        && !trackInfo.title.contains("clean"))
                    continue;

                return audioTrack;
            }
        }
        return null;
    }

    private AudioItem loadItem(String query) {
        var cf = new CompletableFuture<AudioItem>();

        RobertifyAudioManager.getInstance().getAudioPlayerManager().loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                cf.complete(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                cf.complete(playlist);
            }

            @Override
            public void noMatches() {
                cf.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                cf.completeExceptionally(exception);
            }
        });

        return cf.join();
    }

    @Override
    public AudioTrack makeClone() {
        return new RobertifyAudioTrack(trackInfo, youtubeAudioSourceManager, soundCloudAudioSourceManager, id, trackImage, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
