import lombok.extern.slf4j.Slf4j;
import main.audiohandlers.QueueHandler;
import main.audiohandlers.TrackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;

@Slf4j
public class TestTrackScheduler {
    private TrackScheduler underTest;
    private QueueHandler queueHandler;

    @BeforeEach
    void setUp() {
        queueHandler = new QueueHandler();
    }

    @Test
    void testQueueAdd() {
        final var track = UtilsTest.getTestTrack("1");
        queueHandler.add(track);
        assertEquals("Testing Queue Addition", track, queueHandler.poll());
    }

    @Test
    void testQueueAddFail() {
        final var track = UtilsTest.getTestTrack("1");
        final var track2 = UtilsTest.getTestTrack("2");
        queueHandler.add(track2);
        assertNotEquals("Testing Queue Addition Mismatch", track, queueHandler.poll());
    }

    @Test
    void testBulkQueueAdd() {
        final var tracks = UtilsTest.getManyTracks(10);
        queueHandler.addAll(tracks);
        assertEquals("Testing Queue Bulk Addition", tracks, queueHandler.contents());
    }

    @Test
    void testBulkQueueAddFail() {
        final var tracks = UtilsTest.getManyTracks(10);
        final var tracks2 = UtilsTest.getManyTracks(11);
        queueHandler.addAll(tracks2);
        assertNotEquals("Testing Queue Bulk Addition", tracks, queueHandler.contents());
    }

    @Test
    void testQueueBeginningAdd() {
        final var tracks = UtilsTest.getManyTracks(10);
        final var trackToAdd = UtilsTest.getTestTrack("begin");
        queueHandler.addAll(tracks);
        queueHandler.addToBeginning(trackToAdd);
        assertEquals("Testing Queue Beginning Addition", trackToAdd, queueHandler.poll());
    }

    @Test
    void testQueueBeginningFail() {
        final var tracks = UtilsTest.getManyTracks(10);
        final var trackToAdd = UtilsTest.getTestTrack("begin");
        final var trackToAddAgain = UtilsTest.getTestTrack("begin");
        queueHandler.addAll(tracks);
        queueHandler.addToBeginning(trackToAdd);
        queueHandler.addToBeginning(trackToAddAgain);
        assertNotEquals("Testing Queue Beginning Addition Fail", trackToAdd, queueHandler.poll());
    }

    @Test
    void testQueueRemove() {
        final var tracks = UtilsTest.getManyTracks(10);
        queueHandler.addAll(tracks);
        queueHandler.remove(tracks.get(9));

        final var expected = new ArrayList<>(tracks);
        expected.remove(9);
        assertEquals("Testing Queue Remove", expected, queueHandler.contents());
    }

    @Test
    void testPreviousTrackAdd() {
        final var currentTrackList = UtilsTest.getManyTracks(10);
        queueHandler.addAll(currentTrackList);

        final var pastTrack = queueHandler.poll();
        queueHandler.pushPastTrack(pastTrack);
        assertEquals("Testing Previous Track Addition", pastTrack, queueHandler.popPreviousTrack());
    }

    @Test
    void testFullPreviousTrackAdd() {
        final var currentTrackList = UtilsTest.getManyTracks(10);
        queueHandler.addAll(currentTrackList);

        for (int i = 0; i < 10; i++)
            queueHandler.pushPastTrack(queueHandler.poll());

        final var expected = new ArrayList<>(currentTrackList);
        Collections.reverse(expected);
        assertEquals("Testing Full Previous Queue Addition", currentTrackList, queueHandler.previousTracksContent());
    }
}
