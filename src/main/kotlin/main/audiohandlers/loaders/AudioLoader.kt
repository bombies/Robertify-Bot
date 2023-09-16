package main.audiohandlers.loaders

import dev.arbjerg.lavalink.protocol.v4.Exception
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Playlist
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.schlaubi.lavakord.rest.loadItem
import main.audiohandlers.GuildMusicManager
import main.main.Robertify
import net.dv8tion.jda.api.entities.Guild

abstract class AudioLoader(guild: Guild) {
    internal val link = Robertify.lavaKord.getLink(guild.id)
    abstract val query: String
    abstract val musicManager: GuildMusicManager

    open suspend fun loadItem() {
        when (val result = link.loadItem(query)) {
            is LoadResult.TrackLoaded -> {
                onTrackLoad(result.data)
            }

            is LoadResult.PlaylistLoaded -> {
                onPlaylistLoad(result.data)
            }

            is LoadResult.SearchResult -> {
                onSearchResultLoad(result.data.tracks)
            }

            is LoadResult.NoMatches -> {
                onNoMatches()
            }

            is LoadResult.LoadFailed -> {
                onException(result.data)
            }
        }
    }

    abstract suspend fun onPlaylistLoad(playlist: Playlist)
    abstract suspend fun onSearchResultLoad(results: List<Track>)
    abstract suspend fun onTrackLoad(result: Track)
    abstract suspend fun onNoMatches()
    abstract suspend fun onException(exception: Exception)
}