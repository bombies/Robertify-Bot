package main.audiohandlers.utils

import dev.arbjerg.lavalink.client.player.Track
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

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

@OptIn(ExperimentalSerializationApi::class)
val Track.artworkUrl: String?
    get() = try {
        info.artworkUrl
    } catch (e: MissingFieldException) {
        null
    }

val Track.isrc: String?
    get() = info.isrc