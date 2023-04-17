package main.utils.api.robertify.imagebuilders

enum class ImageTypeKt(private vararg val segments: String) {
    NOW_PLAYING("music", "nowplaying"),
    QUEUE("music", "queue");

    fun getSegments(): List<String> = segments.toList()

}