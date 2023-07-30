package main.utils.pagination.pages.queue

import main.commands.slashcommands.audio.QueueCommand
import main.utils.RobertifyEmbedUtils
import main.utils.api.robertify.imagebuilders.builders.QueueImageBuilder
import main.utils.pagination.pages.AbstractImagePage
import main.utils.pagination.pages.MessagePage
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import java.io.InputStream

class QueuePage(
    private val guild: Guild,
    val pageNumber: Int,
    private val _queueItems: MutableList<QueueItem> = mutableListOf()
) : AbstractImagePage() {
    override val embed: MessageEmbed
        get() {
            val content = QueueCommand().getContent(guild, queueItems)
            return RobertifyEmbedUtils.embedMessage(guild, "\t" +
                    "${content.map { str -> "$str\n" }}"
            ).build()
        }

    val queueItems
        get() = _queueItems.toList()

    override val image: InputStream?
        get() {
            val builder = QueueImageBuilder(guild, pageNumber)
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

    fun addItem(item: QueueItem) =
        _queueItems.add(item)
}