package main.audiohandlers.sources;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.*;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import lombok.Getter;
import main.audiohandlers.RobertifyAudioReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobertifyAudioTrack extends DelegatedAudioTrack {
    private final Logger logger = LoggerFactory.getLogger(RobertifyAudioTrack.class);

    private final YoutubeAudioSourceManager youtubeAudioSourceManager;
    private final SoundCloudAudioSourceManager soundCloudAudioSourceManager;
    @Getter
    private final String id;
    @Getter
    private final String trackImage;

    public RobertifyAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager youtubeAudioSourceManager, SoundCloudAudioSourceManager soundCloudAudioSourceManager, String id, String trackImage) {
        super(trackInfo);
        this.youtubeAudioSourceManager = youtubeAudioSourceManager;
        this.soundCloudAudioSourceManager = soundCloudAudioSourceManager;
        this.id = id;
        this.trackImage = trackImage;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        AudioItem item = youtubeAudioSourceManager.loadItem(null, new RobertifyAudioReference(trackInfo.identifier.replaceFirst("ytsearch:", "ytmsearch:"), null, id));

        if (item instanceof AudioPlaylist playlist) {
            final var fallback = playlist.getTracks().get(0);

//            logger.info("[FROM SOURCE] {} - {} [{}]", trackInfo.title, trackInfo.author, trackInfo.length);
//            logger.info("Searching YouTube Music...");
            AudioTrack track = search(playlist);

            if (track == null) {
                item = youtubeAudioSourceManager.loadItem(null, new RobertifyAudioReference(trackInfo.identifier, null, id));

                if (item instanceof AudioPlaylist newPlaylist) {
//                    logger.info("Searching YouTube...");
                    track = search(newPlaylist);
                }
            }

            if (track == null) {
                item = soundCloudAudioSourceManager.loadItem(null, new RobertifyAudioReference(trackInfo.identifier.replaceFirst("ytsearch:", "scsearch:"), null, id));

                if (item instanceof AudioPlaylist newPlaylist) {
//                    logger.info("Searching SoundCloud...");
                    track = search(newPlaylist);
                }
            }

            if (track == null)
                track = fallback;

            if (track instanceof YoutubeAudioTrack ytTrack)
                ytTrack.process(executor);
            else if (track instanceof SoundCloudAudioTrack scTrack)
                scTrack.process(executor);
        }
    }

    private AudioTrack search(AudioPlaylist playlist) {
        for (AudioTrack audioTrack : playlist.getTracks()) {

//            logger.info("{} - {} [{}]", audioTrack.getInfo().title, audioTrack.getInfo().author, audioTrack.getDuration());

            if (audioTrack.getDuration() >= trackInfo.length - 7000
                    && audioTrack.getDuration() <= trackInfo.length + 5000
                    && (audioTrack.getInfo().author.toLowerCase().contains(trackInfo.author.toLowerCase())
                    || audioTrack.getInfo().title.toLowerCase().contains(trackInfo.title.toLowerCase()))
            ) {
                if (audioTrack.getInfo().title.contains("clean")
                        && !trackInfo.title.contains("clean"))
                    continue;

                return audioTrack;
            }
        }
        return null;
    }

    @Override
    public AudioTrack makeClone() {
        return new RobertifyAudioTrack(trackInfo, youtubeAudioSourceManager, soundCloudAudioSourceManager, id, trackImage);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return super.getSourceManager();
    }
}
