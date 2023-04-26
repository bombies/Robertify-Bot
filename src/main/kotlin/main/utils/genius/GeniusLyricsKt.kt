package main.utils.genius

class GeniusLyricsKt(private val gla: GeniusAPIKt, private val path: String, private val id: String) {
    val text: String?
        get() = GeniusLyricsParserKt(gla).get(id)

    override fun toString(): String = path
}