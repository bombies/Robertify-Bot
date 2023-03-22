import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.extern.slf4j.Slf4j;
import main.audiohandlers.TrackScheduler;
import net.dv8tion.jda.api.entities.Guild;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class TestTrackScheduler {
    private Guild testGuild = UtilsTest.getTestGuild();
    private TrackScheduler testScheduler = new TrackScheduler(testGuild, UtilsTest.getTestLink());

    private ConcurrentLinkedQueue<AudioTrack> generateQueue(int size) {
        var queue = testScheduler.getQueue();
        var finalQueue = queue;
        IntStream.range(0, size)
                .forEach(i -> finalQueue.offer(UtilsTest.getTestTrack(String.valueOf(i))));
        return finalQueue;
    }

    @Test
    @DisplayName("Generic Queue Test")
    void testQueue() {
        final var queue = testScheduler.getQueue();
        final var track = UtilsTest.getTestTrack("0");

        queue.offer(track);
        assertEquals(track, queue.poll());
    }

    @Test
    void testBeginningQueue() {
        var queue = testScheduler.getQueue();
        final var track = UtilsTest.getTestTrack("1");
        final var track2 = UtilsTest.getTestTrack("2");

        queue.offer(track);

        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        newQueue.offer(track2);
        newQueue.addAll(queue);
        queue = newQueue;

        assertEquals(track2, queue.peek());
    }

    @Test
    void testBeginningQueueLarge() {
        var queue = testScheduler.getQueue();
        var finalQueue = generateQueue(1000);

        final var newTrack = UtilsTest.getTestTrack("1000");

        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        newQueue.offer(newTrack);
        newQueue.addAll(finalQueue);
        queue = newQueue;

        assertEquals(newTrack, queue.peek());
        queue.poll();
        assertTrue(queue.containsAll(finalQueue));
    }

    @Test
    void testQueueRepeat() {
        var queue = generateQueue(100);
        var savedQueue = new ConcurrentLinkedQueue<>(queue);

        // Test repeating true
        for (int i = 0; i < 3; i++) {
            while (!queue.isEmpty())
                queue.poll();
            queue = new ConcurrentLinkedQueue<>(savedQueue);
        }

        assertTrue(queue.containsAll(savedQueue));
    }
}
