import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import main.utils.resume.GuildResumeCache;
import main.utils.resume.ResumableTrack;
import main.utils.resume.ResumeData;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

@Slf4j
public class ResumeTests {

    @Test
    void testResumableDataToJSON() {
        final var tracks = UtilsTest.getManyTracksWithRequester(10);
        final var resumableTracks = tracks.stream()
                .map(ResumableTrack::new)
                .toList();
        final var resumeData = new ResumeData("vcId", "acId", resumableTracks);
        final var json = resumeData.toString();
        log.info("JSON: {}", json);
        assertNotNull("Checking if JSON is valid", json);
    }

    @Test
    void testResumableDataJSONToObject() {
        final var tracks = UtilsTest.getManyTracksWithRequester(10);
        final var resumableTracks = ResumableTrack.audioTracksToResumableTracks(tracks);
        final var resumeData = new ResumeData("vcId", "acId", resumableTracks);
        final var json = resumeData.toString();

        try {
            final var revertedData = ResumeData.fromJSON(json);

            assertNotNull("Checking if data is null", revertedData);
            assertNotNull("Checking if voice channel ID is null", revertedData.getChannel_id());
            assertEquals("Checking track list length", 10, revertedData.getTracks().size());


            for (int i = 0; i < revertedData.getTracks().size(); i++) {
                assertNotNull("Checking if track %d is not null".formatted(i), revertedData.getTracks().get(i));
            }
        } catch (JsonProcessingException e) {
            log.error("Couldn't load tracks for a test!", e);
        }
    }

    @Test
    void testSingularResumeCacheLoadAndSave() {
        final var resumeCache = new GuildResumeCache("1");
        final var tracks = UtilsTest.getManyTracksWithRequester(10);
        final var resumableTracks = ResumableTrack.audioTracksToResumableTracks(tracks);
        final var resumeData = new ResumeData("vcId", "acId", resumableTracks);

        try {
            resumeCache.setTracks(resumeData);
            final var loadedData = resumeCache.loadData();

            assertEquals("Testing resume cache", resumeData.toString(), loadedData.toString());
        } catch (JsonProcessingException e) {
            log.error("Couldn't load tracks for a test!", e);
        }

    }

    @Test
    void testManyResumeCacheLoadAndSave() {
        final var caches = IntStream.range(0, 10)
                .mapToObj(i -> new GuildResumeCache(String.valueOf(i)));
        final var tracks = UtilsTest.getManyTracksWithRequester(50);
        final var resumableTracks = ResumableTrack.audioTracksToResumableTracks(tracks);
        final var resumeData = new ResumeData("vcID", "acId", resumableTracks);

        caches.forEach(cache -> {
            cache.setTracks(resumeData);
            final ResumeData loadedData;
            try {
                loadedData = cache.loadData();
                assertEquals("Testing resume cache for cache with ID " + cache.getCacheID(), resumeData.toString(), loadedData.toString());

            } catch (JsonProcessingException e) {
                log.error("Couldn't load tracks for a test!", e);
            }
        });
    }
}
