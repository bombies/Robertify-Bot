package main.utils.pagination.pages.queue

import main.utils.api.robertify.imagebuilders.builders.QueueImageBuilderKt
import main.utils.pagination.pages.MessagePageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import java.io.InputStream

class QueuePageKt(private val guild: Guild, val pageNumber: Int, private val _queueItems: MutableList<QueueItemKt> = mutableListOf()) : MessagePageKt {
    override val embed: MessageEmbed
        get() = TODO("Not yet implemented")

    val queueItems
        get() = _queueItems.toList()

    val image: InputStream?
        get() {
            val builder = QueueImageBuilderKt(guild, pageNumber)
            queueItems.forEach { item ->
                builder.addTrack(
                    item.trackIndex,
                    item.trackTitle,
                    item.artist,
                    item.duration
                )
            }
            return builder.build()
        }

    fun addItem(item: QueueItemKt) =
        _queueItems.add(item)
}