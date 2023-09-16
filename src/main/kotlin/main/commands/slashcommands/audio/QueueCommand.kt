package main.commands.slashcommands.audio

import main.audiohandlers.QueueHandler
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.QueueMessages
import main.utils.pagination.PaginationHandler
import main.utils.pagination.pages.queue.QueueItem
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class QueueCommand : AbstractSlashCommand(
    SlashCommand(
    name = "queue",
    description = "See all queued songs"
)
) {
    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val musicManager = RobertifyAudioManager[guild]
        val queueHandler = musicManager.scheduler.queueHandler

        if (queueHandler.isEmpty) {
            event.replyEmbed {
                embed(GeneralMessages.NOTHING_IN_QUEUE)
            }.queue()
            return
        }

        PaginationHandler.paginateQueue(event)
    }

    suspend fun getContent(guild: Guild, queueHandler: QueueHandler): List<String> {
        val content = mutableListOf<String>()
        val trackList = queueHandler.contents.toMutableList()
        val localeManager = LocaleManager[guild]
        trackList.forEachIndexed { i, track ->
            content.add(localeManager.getMessage(QueueMessages.QUEUE_ENTRY,
                Pair("{id}", (i + 1).toString()),
                Pair("{title}", track.title),
                Pair("{author}", track.author),
                Pair("{duration}", GeneralUtils.formatTime(track.length))
            ))
        }
        return content
    }

    suspend fun getContent(guild: Guild, queueItems: List<QueueItem>): List<String> {
        val content = mutableListOf<String>()
        val localeManager = LocaleManager[guild]
        queueItems.forEach { track ->
            content.add(localeManager.getMessage(
                QueueMessages.QUEUE_ENTRY,
                Pair("{id}", track.trackIndex.toString()),
                Pair("{title}", track.trackTitle),
                Pair("{author}", track.artist),
                Pair("{duration}", GeneralUtils.formatTime(track.duration))
            ))
        }
        return content
    }

    suspend fun getPastContent(guild: Guild, queueHandler: QueueHandler): List<String> {
        val content = mutableListOf<String>()
        val trackList = queueHandler.previousTracksContents.toMutableList().reversed()
        val localeManager = LocaleManager[guild]
        trackList.forEachIndexed { i, track ->
            content.add(localeManager.getMessage(QueueMessages.QUEUE_ENTRY,
                Pair("{id}", (i + 1).toString()),
                Pair("{title}", track.title),
                Pair("{author}", track.author),
                Pair("{duration}", GeneralUtils.formatTime(track.length))
            ))
        }
        return content
    }

    override val help: String
        get() = "Shows all the queued songs"
}