package main.commands.slashcommands.audio

import main.audiohandlers.QueueHandlerKt
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.QueueMessages
import main.utils.pagination.PaginationHandlerKt
import main.utils.pagination.pages.queue.QueueItemKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class QueueCommandKt : AbstractSlashCommandKt(
    CommandKt(
    name = "queue",
    description = "See all queued songs"
)
) {
    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val musicManager = RobertifyAudioManagerKt[guild]
        val queueHandler = musicManager.scheduler.queueHandler

        if (queueHandler.isEmpty) {
            event.replyWithEmbed(guild) {
                embed(GeneralMessages.NOTHING_IN_QUEUE)
            }.queue()
            return
        }

        PaginationHandlerKt.paginateQueue(event)
    }

    fun getContent(guild: Guild, queueHandler: QueueHandlerKt): List<String> {
        val content = mutableListOf<String>()
        val trackList = queueHandler.contents.toMutableList()
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        trackList.forEachIndexed { i, track ->
            content.add(localeManager.getMessage(QueueMessages.QUEUE_ENTRY,
                Pair("{id}", (i + 1).toString()),
                Pair("{title}", track.title),
                Pair("{author}", track.author),
                Pair("{duration}", GeneralUtilsKt.formatTime(track.length))
            ))
        }
        return content
    }

    fun getContent(guild: Guild, queueItems: List<QueueItemKt>): List<String> {
        val content = mutableListOf<String>()
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        queueItems.forEach { track ->
            content.add(localeManager.getMessage(
                QueueMessages.QUEUE_ENTRY,
                Pair("{id}", track.trackIndex.toString()),
                Pair("{title}", track.trackTitle),
                Pair("{author}", track.artist),
                Pair("{duration}", GeneralUtilsKt.formatTime(track.duration))
            ))
        }
        return content
    }

    fun getPastContent(guild: Guild, queueHandler: QueueHandlerKt): List<String> {
        val content = mutableListOf<String>()
        val trackList = queueHandler.previousTracksContents.toMutableList().reversed()
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        trackList.forEachIndexed { i, track ->
            content.add(localeManager.getMessage(QueueMessages.QUEUE_ENTRY,
                Pair("{id}", (i + 1).toString()),
                Pair("{title}", track.title),
                Pair("{author}", track.author),
                Pair("{duration}", GeneralUtilsKt.formatTime(track.length))
            ))
        }
        return content
    }

    override val help: String
        get() = "Shows all the queued songs"
}