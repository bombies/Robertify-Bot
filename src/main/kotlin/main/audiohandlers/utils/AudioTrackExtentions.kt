package main.audiohandlers.utils

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

val AudioTrack.title: String
    get() = info.title

val AudioTrack.author: String
    get() = info.author

val AudioTrack.length: Long
    get() = info.length

val AudioTrack.isStream: Boolean
    get() = info.isStream

val AudioTrack.uri: String
    get() = info.uri

val AudioTrack.source: String
    get() = sourceManager.sourceName