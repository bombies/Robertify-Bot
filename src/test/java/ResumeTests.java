import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import main.utils.resume.ResumableTrack;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

@Slf4j
public class ResumeTests {

    @Test
    void testResumableTracksToJSON() {
        final var tracks = UtilsTest.getManyTracks(10);
        final var resumableTracks = tracks.stream()
                .map(ResumableTrack::new)
                .toList();
        final var json = ResumableTrack.collectionToString(resumableTracks);
        log.info("JSON: {}", json);
        assertNotNull("Checking if JSON is valid", json);
    }

    @Test
    void testResumableTrackJSONToObject() {
        final var tracks = UtilsTest.getManyTracks(10);
        final var resumableTracks = ResumableTrack.audioTracksToResumableTracks(tracks);
        final var json = ResumableTrack.collectionToString(resumableTracks);
        final var revertedTracks = ResumableTrack.stringToList(json);

        assertNotNull("Checking if list is null", revertedTracks);
        assertEquals("Checking list length", 10, revertedTracks.size());

        for (int i = 0; i < revertedTracks.size(); i++) {
            log.info("Revered track {}: {}", i, revertedTracks.get(i));
            assertNotNull("Checking if track %d is not null".formatted(i), revertedTracks.get(i));
        }
    }
}
