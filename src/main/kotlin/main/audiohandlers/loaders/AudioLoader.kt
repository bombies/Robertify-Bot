package main.audiohandlers.loaders

import dev.schlaubi.lavakord.Exception
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.audio.player.Player
import dev.schlaubi.lavakord.rest.loadItem
import dev.schlaubi.lavakord.rest.models.PartialTrack
import dev.schlaubi.lavakord.rest.models.TrackResponse

abstract class AudioLoader protected constructor(private val link: Link) {
    abstract val query: String
    private lateinit var loadedItem: TrackResponse

    suspend fun loadItem() {
        loadedItem = link.loadItem(query)
        handleLoadType()
    }

    private suspend fun handleLoadType() = when (loadedItem.loadType) {
        TrackResponse.LoadType.TRACK_LOADED -> trackLoaded(link.player, loadedItem.track)
        TrackResponse.LoadType.PLAYLIST_LOADED -> playlistLoaded(link.player, loadedItem.tracks, loadedItem.playlistInfo)
        TrackResponse.LoadType.SEARCH_RESULT -> searchLoaded(link.player, loadedItem.tracks)
        TrackResponse.LoadType.NO_MATCHES -> noMatches()
        TrackResponse.LoadType.LOAD_FAILED -> loadFailed(loadedItem.exception)
    }

    abstract suspend fun trackLoaded(player: Player, track: PartialTrack)
    abstract suspend fun playlistLoaded(player: Player, tracks: List<PartialTrack>, playlistInfo: TrackResponse.NullablePlaylistInfo)
    abstract suspend fun searchLoaded(player: Player, tracks: List<PartialTrack>)
    abstract suspend fun noMatches()
    abstract suspend fun loadFailed(exception: Exception?)
}