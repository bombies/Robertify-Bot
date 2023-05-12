import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState
import main.audiohandlers.models.Requester
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

internal fun mockAudioTrack(id: String): AudioTrack {
    val track = mock<AudioTrack> {
        on { info } doReturn AudioTrackInfo(
            "test title",
            "test author",
            10000,
            id,
            false,
            "http://testuri:80"
        )

        on { state } doReturn AudioTrackState.PLAYING
        on { identifier } doReturn id
        on { isSeekable } doReturn false
        on { position } doReturn 0
        on { sourceManager } doReturn null
        on { makeClone() } doReturn null
    }

    verify(track, never()).info
    return track
}

internal fun mockAudioTracks(count: Int) = (0 until count).map { mockAudioTrack(it.toString()) }

internal fun mockAudioTracksWithRequester(count: Int) = mockAudioTracks(count).map { it.withRequester() }

internal fun AudioTrack.withRequester() = Pair(this, Requester("userid", identifier))