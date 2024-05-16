package main.audiohandlers.loaders

import dev.arbjerg.lavalink.client.player.*
import dev.arbjerg.lavalink.protocol.v4.PlaylistInfo
import main.audiohandlers.GuildMusicManager
import main.main.Robertify
import net.dv8tion.jda.api.entities.Guild

abstract class AudioLoader(private val guild: Guild) {
    internal val link
        get() = Robertify.lavalink.getOrCreateLink(guild.idLong)
    abstract val query: String
    abstract val musicManager: GuildMusicManager

    open fun loadItem() {
        link.loadItem(query)
            .subscribe itemLoad@{ item ->
                when (item) {
                    is TrackLoaded -> {
                        onTrackLoad(item.track)
                    }

                    is PlaylistLoaded -> {
                        onPlaylistLoad(item.tracks, item.info)
                    }

                    is SearchResult -> {
                        onSearchResultLoad(item.tracks)
                    }

                    is NoMatches -> {
                        onNoMatches()
                    }

                    is LoadFailed -> {
                        onException(item.exception)
                    }
                }
            }
    }

    abstract fun onPlaylistLoad(playlist: List<Track>, playlistInfo: PlaylistInfo)
    abstract fun onSearchResultLoad(results: List<Track>)
    abstract fun onTrackLoad(result: Track)
    abstract fun onNoMatches()
    abstract fun onException(exception: TrackException)
}