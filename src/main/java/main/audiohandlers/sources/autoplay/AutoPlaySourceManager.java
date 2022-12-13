package main.audiohandlers.sources.autoplay;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import main.audiohandlers.sources.RobertifyAudioSourceManager;
import main.audiohandlers.sources.RobertifyAudioTrack;
import main.utils.resume.ResumeData;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AutoPlaySourceManager extends RobertifyAudioSourceManager {
    private final static Logger logger = LoggerFactory.getLogger(AutoPlaySourceManager.class);
    public final static String SEARCH_PREFIX = "ap:";
    private final AudioPlayerManager audioPlayerManager;

    public AutoPlaySourceManager(AudioPlayerManager audioPlayerManager) {
        super(audioPlayerManager);
        this.audioPlayerManager = audioPlayerManager;
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        logger.debug("Attempting to load using AutoPlaySourceManager...");

        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                final JSONArray arr = new JSONArray(reference.identifier.replaceFirst(SEARCH_PREFIX, ""));
                final var autoplayData = new ResumeData();
                final List<AudioTrack> queue = new ArrayList<>(autoplayData.assembleSpotifyTracks(arr));

                return new BasicAudioPlaylist("Recommended Tracks", queue.subList(1, queue.size()), queue.get(0), false);
            } else {
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String getSearchPrefix() {
        return SEARCH_PREFIX;
    }

    @Override
    public String getSourceName() {
        return "autoplay";
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
    public void shutdown() {

    }
}
