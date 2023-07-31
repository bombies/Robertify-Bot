package main.utils.pagination.pages.queue

data class QueueItem(
    val trackIndex: Int,
    val trackTitle: String,
    val artist: String,
    val duration: Long
)
