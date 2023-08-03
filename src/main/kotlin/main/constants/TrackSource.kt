package main.constants

import java.util.*


enum class TrackSource {
    SPOTIFY,
    DEEZER,
    YOUTUBE,
    SOUNDCLOUD,
    APPLE_MUSIC,
    RESUMED;

    companion object {
        fun parse(name: String): TrackSource {
            return when (name.lowercase(Locale.getDefault())) {
                "youtube" -> YOUTUBE
                "spotify" -> SPOTIFY
                "deezer" -> DEEZER
                "soundcloud" -> SOUNDCLOUD
                "applemusic", "apple_music" -> APPLE_MUSIC
                "resumed" -> RESUMED
                else -> throw NullPointerException("There is no such source")
            }
        }
    }
}
