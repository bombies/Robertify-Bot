package main.utils.genius

import java.io.IOException
import java.net.URL
import java.util.*

class GeniusLyricsParserKt(private val gla: GeniusAPIKt) {

    companion object {
        private val GENIUS_EMBED_URL_HEAD = "https://genius.com/songs/"
        private val GENIUS_EMBED_URL_TAIL = "/embed.js"
    }

    fun get(id: String) =
        parseLyrics(id)

    private fun parseLyrics(id: String): String? {
        try {
            val connection = URL(GENIUS_EMBED_URL_HEAD + id + GENIUS_EMBED_URL_TAIL)
                .openConnection()
            connection.setRequestProperty("User-Agent", "1.1.2")

            val scanner = Scanner(connection.getInputStream())
            scanner.useDelimiter("\\A".toPattern())
            val raw = StringBuilder()

            while (scanner.hasNext())
                raw.append(scanner.next())

            if (raw.toString().isEmpty() || raw.toString().isBlank())
                return null
            return getReadable(raw.toString())
        } catch (e: IOException) {
            return null
        }
    }

    private fun getReadable(rawLyrics: String): String? {
        //Remove start
        var lyrics = rawLyrics
        lyrics = lyrics.replace(
            "[\\S\\s]*<div class=\\\\\\\\\\\\\"rg_embed_body\\\\\\\\\\\\\">[ (\\\\\\\\n)]*".toRegex(),
            ""
        )
        //Remove end
        lyrics = lyrics.replace("[ (\\\\\\\\n)]*<\\\\/div>[\\S\\s]*".toRegex(), "")
        //Remove tags between
        lyrics = lyrics.replace("<[^<>]*>".toRegex(), "")
        //Unescape spaces
        lyrics = lyrics.replace("\\\\\\\\n".toRegex(), "\n")
        //Unescape '
        lyrics = lyrics.replace("\\\\'".toRegex(), "'")
        //Unescape "
        lyrics = lyrics.replace("\\\\\\\\\\\\\"".toRegex(), "\"")
        return lyrics
    }
}