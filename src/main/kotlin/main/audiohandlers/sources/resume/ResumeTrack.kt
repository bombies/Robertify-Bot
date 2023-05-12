package main.audiohandlers.sources.resume

import com.github.topisenpai.lavasrc.mirror.MirroringAudioSourceManager
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo

data class ResumeTrack(
    val audioTrackInfo: AudioTrackInfo,
    val isrc: String?,
    val artwork: String?,
    val sourceManager: MirroringAudioSourceManager
) : MirroringAudioTrack(audioTrackInfo, isrc, artwork, sourceManager)
