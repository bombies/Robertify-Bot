package main.utils.genius

class GeniusLyrics(private val gla: GeniusAPI, private val path: String, private val id: String) {
    val text: String?
        get() = GeniusLyricsParser(gla).get(id)

    override fun toString(): String = path
}