package main.audiohandlers.loaders

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import main.audiohandlers.GuildMusicManagerKt
import main.audiohandlers.RobertifyAudioManagerKt

abstract class AudioLoader : AudioLoadResultHandler {
    companion object {
        private val playerManager: AudioPlayerManager
            get() = RobertifyAudioManagerKt.playerManager
    }

    abstract val query: String
    abstract val musicManager: GuildMusicManagerKt

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