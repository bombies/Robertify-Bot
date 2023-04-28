package main.constants

import java.util.*


enum class TrackSourceKt {
    SPOTIFY,
    DEEZER,
    YOUTUBE,
    SOUNDCLOUD,
    APPLE_MUSIC,
    RESUMED;

    companion object {
        fun parse(name: String): TrackSourceKt {
            return when (name.lowercase(Locale.getDefault())) {
                "youtube" -> YOUTUBE
                "spotify" -> SPOTIFY
                "deezer" -> DEEZER
                "soundcloud" -> SOUNDCLOUD
                "applemusic", "apple_music" -> APPLE_MUSIC
                else -> throw NullPointerException("There is no such source")
            }
        }
    }
}
