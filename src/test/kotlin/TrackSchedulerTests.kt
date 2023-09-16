import main.audiohandlers.QueueHandler
import kotlin.test.*

class TrackSchedulerTests {

    private lateinit var queueHandler: QueueHandler

    @BeforeTest
    fun setUp() {
        queueHandler = QueueHandler()
    }

    @Test
    fun testQueueAdd() {
        val track = mockAudioTrack("1")
        queueHandler.add(track)
        assertEquals(track, queueHandler.poll(), "Testing queue addition")
    }

    @Test
    fun testQueueAddFail() {
        val track = mockAudioTrack("1")
        val track2 = mockAudioTrack("2")
        queueHandler.add(track)
        queueHandler.add(track2)
        assertNotEquals(track2, queueHandler.poll(), "Testing queue addition mismatch")
    }

    @Test
    fun testBulkQueueAdd() {
        val tracks = mockAudioTracks(10)
        queueHandler.addAll(tracks)
        assertEquals(tracks, queueHandler.contents, "Testing queue bulk addition")
    }

    @Test
    fun testQueueBeginningAdd() {
        val tracks = mockAudioTracks(10)
        val beginningTrack = mockAudioTrack("begin")
        queueHandler.addAll(tracks)
        queueHandler.addToBeginning(beginningTrack)
        assertEquals(beginningTrack, queueHandler.poll(), "Testing queue beginning addition")
    }

    @Test
    fun testQueueRemove() {
        val tracks = mockAudioTracks(10)
        queueHandler.addAll(tracks)
        queueHandler.remove(tracks[9])

        val expected = tracks.subList(0, 9)
        assertEquals(expected, queueHandler.contents, "Testing queue remove")
    }

    @Test
    fun testPreviousTrackAdd() {
        val currentTrackList = mockAudioTracks(10)
        queueHandler.addAll(currentTrackList)

        val pastTrack = queueHandler.poll()

        assertNotNull(pastTrack, "Testing if the past track is null")
        queueHandler.pushPastTrack(pastTrack)
        assertEquals(pastTrack, queueHandler.popPreviousTrack(), "Testing previous track addition")
    }

    @Test
    fun testFullPreviousTrackAdd() {
        val currentTrackList = mockAudioTracks(10)
        queueHandler.addAll(currentTrackList)

        (0 until 10).forEach { _ ->
            val polledTrack = queueHandler.poll()
            assertNotNull(polledTrack, "Testing if the past track is null")
            queueHandler.pushPastTrack(polledTrack)
        }

        val expected = currentTrackList.reversed()
        assertEquals(expected, queueHandler.previousTracksContents, "Testing full previous queue addition")
    }
}