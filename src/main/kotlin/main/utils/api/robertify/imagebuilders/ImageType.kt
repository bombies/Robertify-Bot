package main.utils.api.robertify.imagebuilders

enum class ImageType(private vararg val segments: String) {
    NOW_PLAYING("music", "nowplaying"),
    QUEUE("music", "queue"),
    PLAYLISTS_LIST("playlists", "list"),
    PLAYLISTS_CONTENT("playlists", "content");

    fun getSegments(): List<String> = segments.toList()

}