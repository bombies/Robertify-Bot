package main.utils.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.List;

public class ResumeData {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    private final String channel_id;
    @Getter
    private final List<ResumableTrack> tracks;

    public ResumeData() {
        this.channel_id = null;
        this.tracks = null;
    }

    public ResumeData(String channel_id, List<ResumableTrack> tracks) {
        this.channel_id = channel_id;
        this.tracks = tracks;
    }

    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static ResumeData fromJSON(String json) throws JsonProcessingException {
        return mapper.readValue(json, ResumeData.class);
    }
}
