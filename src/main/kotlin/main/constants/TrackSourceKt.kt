package main.constants

import java.util.*


enum class TrackSourceKt {
    SPOTIFY,
    DEEZER,
    YOUTUBE,
    SOUNDCLOUD;

    companion object {
        fun parse(name: String): TrackSourceKt {
            return when (name.lowercase(Locale.getDefault())) {
                "youtube" -> YOUTUBE
                "spotify" -> SPOTIFY
                "deezer" -> DEEZER
                "soundcloud" -> SOUNDCLOUD
                else -> throw NullPointerException("There is no such source")
            }
        }
    }
}
