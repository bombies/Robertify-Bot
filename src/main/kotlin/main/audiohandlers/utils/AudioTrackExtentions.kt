package main.audiohandlers.utils

import dev.arbjerg.lavalink.protocol.v4.Track
import dev.schlaubi.lavakord.plugins.lavasrc.lavaSrcInfo

val Track.identifier: String
    get() = info.identifier

val Track.title: String
    get() = info.title

val Track.author: String
    get() = info.author

val Track.length: Long
    get() = info.length

val Track.isStream: Boolean
    get() = info.isStream

val Track.uri: String?
    get() = info.uri

val Track.source: String
    get() = info.sourceName

val Track.artworkUrl: String?
    get() = info.artworkUrl ?: lavaSrcInfo.artistArtworkUrl

val Track.isrc: String?
    get() = info.isrc