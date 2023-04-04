package main.audiohandlers.sources.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.topisenpai.lavasrc.mirror.DefaultMirroringAudioTrackResolver;
import com.github.topisenpai.lavasrc.mirror.MirroringAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.Getter;
import main.utils.resume.ResumableTrack;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

public class ResumeSourceManager extends MirroringAudioSourceManager {
    private final Logger logger = LoggerFactory.getLogger(ResumeSourceManager.class);
    public static final String SEARCH_PREFIX = "rsearch:";

    @Getter
    private final AudioPlayerManager audioPlayerManager;

    public ResumeSourceManager(AudioPlayerManager audioPlayerManager) {
        super(audioPlayerManager, new DefaultMirroringAudioTrackResolver(new String[]{}));
        this.audioPlayerManager = audioPlayerManager;
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            if (!reference.identifier.startsWith(SEARCH_PREFIX))
                return null;

            final var object = new JSONArray(reference.identifier.replaceAll(SEARCH_PREFIX, ""));
            final var queue = new ArrayList<AudioTrack>();
            object.spliterator()
                    .forEachRemaining(o -> {
                        try {
                            final var resumableTrack = ResumableTrack.fromJSON(o.toString());
                            queue.add(resumableTrack.toAudioTrack(this));
                        } catch (JsonProcessingException e) {
                            logger.warn("Could not parse a track! Data: {}", o);
                        }
                    });

            if (queue.size() == 0)
                return null;

            if (queue.size() > 1)
                return new BasicAudioPlaylist("Resumed Queue", queue, null, false);
            else return queue.get(0);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return null;
        }
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new ResumeTrack(
                trackInfo,
                DataFormatTools.readNullableText(input),
                DataFormatTools.readNullableText(input),
                this
        );
    }

    @Override
    public void shutdown() {

    }

    @Override
    public String getSourceName() {
        return "resume";
    }
}
