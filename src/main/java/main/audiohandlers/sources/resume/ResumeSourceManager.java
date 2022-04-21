package main.audiohandlers.sources.resume;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.Getter;
import main.audiohandlers.sources.RobertifyAudioSourceManager;
import main.audiohandlers.sources.RobertifyAudioTrack;
import main.utils.resume.ResumeData;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResumeSourceManager extends RobertifyAudioSourceManager {
    private final Logger logger = LoggerFactory.getLogger(ResumeSourceManager.class);
    public static final String SEARCH_PREFIX = "rsearch:";

    @Getter
    private final AudioPlayerManager audioPlayerManager;

    public ResumeSourceManager(AudioPlayerManager audioPlayerManager) {
        super(audioPlayerManager);
        this.audioPlayerManager = audioPlayerManager;
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        logger.debug("Attempting to load using ResumeSourceManager...");
        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                logger.debug("Using ResumeSourceManager as the source");
                logger.debug("IDENTIFIER: {}", reference.identifier);

                JSONObject object = new JSONObject(reference.identifier.replaceFirst(SEARCH_PREFIX, ""));
                logger.debug("JSON OBJECT: {}", object);

                ResumeData resumeData = new ResumeData();

                final AudioTrack playingTrack = resumeData.assembleTrack(object.getJSONObject(ResumeData.Fields.PLAYING_TRACK.toString()), true);
                final List<AudioTrack> queue = new ArrayList<>(resumeData.assembleSpotifyTracks(object.getJSONArray(ResumeData.Fields.QUEUE.toString())));

                for (var obj : object.getJSONArray(ResumeData.Fields.QUEUE.toString())) {
                    final JSONObject trackObj = (JSONObject) obj;
                    AudioTrack track = resumeData.assembleTrack(trackObj, false);
                    if (track != null)
                            queue.add(track);
                }

                logger.debug("There are {} items in the queue", queue.size());
                return new BasicAudioPlaylist("Queue before restart", queue, playingTrack, false);
            } else {
                logger.debug("Can't use ResumeSourceManager as a source!");
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String getSearchPrefix() {
        return "rsearch";
    }

    @Override
    public String getSourceName() {
        return "resume";
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {

    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new RobertifyAudioTrack(trackInfo, null, null, this);
    }

    @Override
    public void shutdown() {}
}
