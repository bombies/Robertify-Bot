package main.utils.genius

import dev.minn.jda.ktx.util.SLF4J
import main.constants.BotConstantsKt
import main.main.ConfigKt
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.properties.Delegates

class GeniusSongSearchKt(private val gla: GeniusAPIKt, query: String, page: Int = 1) {

    var status by Delegates.notNull<Int>()
    var nextPage by Delegates.notNull<Int>()
    val hits = LinkedList<Hit>()

    companion object {
        private val logger by SLF4J
    }

    init {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)

        try {
            val uri = URI("https://genius.com/api/search/song?page=$page&q=$encodedQuery")
            request(uri)
        } catch (e: URISyntaxException) {
            logger.error("Invalid URI!", e)
        }
    }

    private fun request(uri: URI) {
        try {
            val request = Request.Builder()
                .url(uri.toURL())
                .get()
                .addHeader("User-Agent", BotConstantsKt.USER_AGENT)
                .addHeader("Content-Type","application/json")
                .addHeader("Accept","application/json")
                .addHeader("x-rapidapi-host", "genius.p.rapidapi.com")
                .addHeader("x-rapidapi-key", ConfigKt.GENIUS_API_KEY)
                .build();

            val result = try {
                val response = OkHttpClient().newCall(request).execute()
                response.body()?.string()
            } catch (e: Exception) {
                logger.error("Unexpected error", e)
                return
            }

            parse(JSONObject(result))
        } catch (e: MalformedURLException) {
            throw InternalError(e)
        }
    }

    private fun parse(jRoot: JSONObject) {
        this.status = jRoot.getJSONObject("meta").getInt("status")
        val response = jRoot.getJSONObject("response")
        if (!response.isNull("next_page")) {
            this.nextPage = response.getInt("next_page")
        }
        val section = response.getJSONArray("sections").getJSONObject(0)
        val hits = section.getJSONArray("hits")
        for (i in 0 until hits.length()) {
            val hitRoot = hits.getJSONObject(i).getJSONObject("result")
            this.hits.add(Hit(gla, hitRoot))
        }
    }


    class Hit(private val gla: GeniusAPIKt, jRoot: JSONObject) {
        val id: Long
        val title: String
        val titleWithFeatured: String
        val url: String
        val imageUrl: String
        val thumbnailUrl: String
        val artist: Artist

        init {
            id = jRoot.getLong("id")
            title = jRoot.getString("title")
            titleWithFeatured = jRoot.getString("title_with_featured")
            url = jRoot.getString("url")
            imageUrl = jRoot.getString("header_image_url")
            thumbnailUrl = jRoot.getString("song_art_image_thumbnail_url")
            artist = Artist(jRoot.getJSONObject("primary_artist"))
        }

        fun fetchLyrics(): String? =
            GeniusLyricsParserKt(gla).get(id.toString() + "")

    }


    class Artist(jRoot: JSONObject) {
        val id: Long
        val imageUrl: String
        val name: String
        val slug: String
        val url: String

        init {
            id = jRoot.getLong("id")
            imageUrl = jRoot.getString("image_url")
            name = jRoot.getString("name")
            slug = jRoot.getString("slug")
            url = jRoot.getString("url")
        }
    }

}