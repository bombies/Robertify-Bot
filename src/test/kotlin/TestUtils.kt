import dev.arbjerg.lavalink.client.protocol.Track
import dev.arbjerg.lavalink.protocol.v4.TrackInfo
import main.audiohandlers.models.Requester
import main.audiohandlers.utils.identifier
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

internal fun mockAudioTrack(id: String): Track {
    val track = mock<Track> {
        on { info } doReturn TrackInfo(
            id,
            true,
            "test author",
            10000,
            false,
            0,
            "test title",
            "http://testuri:80",
            "mock",
            null,
            null
        )
    }

    verify(track, never()).info
    return track
}

internal fun mockAudioTracks(count: Int) = (0 until count).map { mockAudioTrack(it.toString()) }

internal fun mockAudioTracksWithRequester(count: Int) = mockAudioTracks(count).map { it.withRequester() }

internal fun Track.withRequester() = Pair(this, Requester("userid", identifier))