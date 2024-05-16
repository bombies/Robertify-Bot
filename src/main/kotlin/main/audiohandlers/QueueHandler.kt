package main.audiohandlers

import dev.arbjerg.lavalink.client.player.Track
import java.util.Stack
import java.util.concurrent.ConcurrentLinkedQueue

class QueueHandler {
    private val queue = ConcurrentLinkedQueue<Track>()
    private val savedQueue = ConcurrentLinkedQueue<Track>()
    private val previousTracks = Stack<Track>()
    var lastPlayedTrackBuffer: Track? = null
    var trackRepeating: Boolean = false
    var queueRepeating: Boolean = false

    val size: Int
        get() = queue.size

    val previousTracksSize: Int
        get() = previousTracks.size

    val contents: List<Track>
        get() = queue.toList()

    val previousTracksContents: List<Track>
        get() = previousTracks.toList().reversed()

    val isEmpty: Boolean
        get() = queue.isEmpty()

    val isPreviousTracksEmpty: Boolean
        get() = previousTracks.isEmpty()

    fun add(track: Track): Boolean = queue.offer(track)

    fun addAll(tracks: Collection<Track>): Boolean = queue.addAll(tracks)

    fun setSavedQueue(tracks: Collection<Track>) {
        if (!queueRepeating)
            return
        savedQueue.addAll(tracks)
    }

    fun loadSavedQueue() {
        queue.clear()
        queue.addAll(savedQueue)
    }

    fun pushPastTrack(track: Track): Track = previousTracks.push(track)

    fun remove(track: Track): Boolean = queue.remove(track)

    fun removeAll(tracks: Collection<Track>): Boolean = queue.removeAll(tracks.toSet())

    fun poll(): Track? = queue.poll()

    fun popPreviousTrack(): Track? = previousTracks.pop()

    fun clear() = queue.clear()

    fun clearSavedQueue() = savedQueue.clear()

    fun clearPreviousTracks() = previousTracks.clear()

    fun addToBeginning(track: Track) {
        val newQueue = ConcurrentLinkedQueue<Track>()
        newQueue.offer(track)
        newQueue.addAll(queue)
        queue.clear()
        queue.addAll(newQueue)
    }

    fun addToBeginning(tracks: Collection<Track>) {
        val newQueue = ConcurrentLinkedQueue<Track>()
        newQueue.addAll(tracks)
        newQueue.addAll(queue)
        queue.clear()
        queue.addAll(newQueue)
    }
}