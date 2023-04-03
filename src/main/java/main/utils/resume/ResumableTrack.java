package main.utils.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class ResumableTrack {
    private final static ObjectMapper mapper = new ObjectMapper();

    @Getter
    private final AudioTrackInfoWrapper info;
    @Getter
    @Nullable
    private final String artworkURL;
    @Getter
    @Nullable
    private final String isrc;

    public ResumableTrack() {
        this.artworkURL = null;
        this.isrc = null;
        this.info = null;
    }

    public ResumableTrack(@Nullable String artworkURL, @Nullable String isrc, AudioTrackInfoWrapper info) {
        this.artworkURL = artworkURL;
        this.isrc = isrc;
        this.info = info;
    }

    public ResumableTrack(AudioTrack track) {
        this.info = new AudioTrackInfoWrapper(track.getInfo());
        if (track instanceof MirroringAudioTrack mt) {
            this.artworkURL = mt.getArtworkURL();
            this.isrc = mt.getISRC();
        } else {
            this.artworkURL = null;
            this.isrc = null;
        }
    }

    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static List<ResumableTrack> audioTracksToResumableTracks(List<AudioTrack> tracks) {
        return tracks.stream()
                .map(track -> new ResumableTrack(track))
                .toList();
    }

    public static String collectionToString(Collection<ResumableTrack> tracks) {
        final var arr = new JSONArray();
        tracks.forEach(track -> arr.put(new JSONObject(track.toString())));
        return arr.toString();
    }

    public static List<ResumableTrack> stringToList(String array) {
        final var revertedTracks = new ArrayList<ResumableTrack>();

        new JSONArray(array)
                .spliterator()
                .forEachRemaining(obj -> {
                    try {
                        revertedTracks.add(ResumableTrack.fromJSON(obj.toString()));
                    } catch (JsonProcessingException e) {
                        log.error("Error processing JSON", e);
                    }
                });

        return revertedTracks;
    }

    public static ResumableTrack fromJSON(String json) throws JsonProcessingException {
        return mapper.readValue(json, ResumableTrack.class);
    }

    public static class AudioTrackInfoWrapper extends AudioTrackInfo {

        public AudioTrackInfoWrapper() {
            super(null, null, 0, null, false, null);
        }

        public AudioTrackInfoWrapper(AudioTrackInfo info) {
            super(info.title, info.author, info.length, info.identifier, info.isStream, info.uri);
        }
    }
}
