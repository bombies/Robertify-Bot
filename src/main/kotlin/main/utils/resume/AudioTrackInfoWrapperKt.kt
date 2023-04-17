package main.utils.resume

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo

data class AudioTrackInfoWrapperKt(val info: AudioTrackInfo) : AudioTrackInfo(info.title, info.author, info.length, info.identifier, info.isStream, info.uri)
