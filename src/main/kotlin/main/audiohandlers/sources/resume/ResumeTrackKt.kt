package main.audiohandlers.sources.resume

import com.github.topisenpai.lavasrc.mirror.MirroringAudioSourceManager
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo

data class ResumeTrackKt(
    val info: AudioTrackInfo,
    val isrc: String?,
    val artworkURL: String?,
    val sourceManager: MirroringAudioSourceManager
) : MirroringAudioTrack(info, isrc, artworkURL, sourceManager)
