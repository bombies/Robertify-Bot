package main.audiohandlers

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.Stack
import java.util.concurrent.ConcurrentLinkedQueue

class QueueHandlerKt {
    private val queue = ConcurrentLinkedQueue<AudioTrack>()
    private val savedQueue = ConcurrentLinkedQueue<AudioTrack>()
    private val previousTracks = Stack<AudioTrack>()
    var lastPlayedTrackBuffer: AudioTrack? = null
    var trackRepeating: Boolean = false
    var queueRepeating: Boolean = false

    val size: Int
        get() = queue.size

    val previousTracksSize: Int
        get() = previousTracks.size

    val contents: List<AudioTrack>
        get() = queue.toList()

    val previousTracksContents: List<AudioTrack>
        get() = previousTracks.toList()

    val isEmpty: Boolean
        get() = queue.isEmpty()

    val isPreviousTracksEmpty: Boolean
        get() = previousTracks.isEmpty()

    fun add(track: AudioTrack): Boolean = queue.offer(track)

    fun addAll(tracks: Collection<AudioTrack>): Boolean = queue.addAll(tracks)

    fun setSavedQueue(tracks: Collection<AudioTrack>) {
        if (!queueRepeating)
            return
        savedQueue.addAll(tracks)
    }

    fun loadSavedQueue() {
        queue.clear()
        queue.addAll(savedQueue)
    }

    fun pushPastTrack(track: AudioTrack) = previousTracks.push(track)

    fun remove(track: AudioTrack): Boolean = queue.remove(track)

    fun removeAll(tracks: Collection<AudioTrack>): Boolean = queue.removeAll(tracks.toSet())

    fun poll(): AudioTrack? = queue.poll()

    fun popPreviousTrack() = previousTracks.pop()

    fun clear() = queue.clear()

    fun clearSavedQueue() = savedQueue.clear()

    fun clearPreviousTracks() = previousTracks.clear()

    fun addToBeginning(track: AudioTrack) {
        val newQueue = ConcurrentLinkedQueue<AudioTrack>()
        newQueue.offer(track)
        newQueue.addAll(queue)
        queue.clear()
        queue.addAll(newQueue)
    }

    fun addToBeginning(tracks: Collection<AudioTrack>) {
        val newQueue = ConcurrentLinkedQueue<AudioTrack>()
        newQueue.addAll(tracks)
        newQueue.addAll(queue)
        queue.clear()
        queue.addAll(newQueue)
    }
}