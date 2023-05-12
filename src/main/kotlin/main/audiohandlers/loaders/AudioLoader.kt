package main.audiohandlers.loaders

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import main.audiohandlers.GuildMusicManager
import main.audiohandlers.RobertifyAudioManager

abstract class AudioLoader : AudioLoadResultHandler {
    companion object {
        private val playerManager: AudioPlayerManager
            get() = RobertifyAudioManager.playerManager
    }

    abstract val query: String
    abstract val musicManager: GuildMusicManager

    fun loadItem() {
        playerManager.loadItemOrdered(musicManager, query, this)
    }

    abstract fun onPlaylistLoad(playlist: AudioPlaylist)
    abstract fun onSearchResultLoad(results: AudioPlaylist)

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult)
            onSearchResultLoad(results = playlist)
        else onPlaylistLoad(playlist = playlist)
    }
}