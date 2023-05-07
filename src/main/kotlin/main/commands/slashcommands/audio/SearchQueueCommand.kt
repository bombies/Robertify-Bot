package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.toMention
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.SearchQueueMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class SearchQueueCommand : AbstractSlashCommandKt(
    CommandKt(
        name = "searchqueue",
        description = "Search your queue for a specific track and all of the current information.",
        options = listOf(
            CommandOptionKt(
                name = "title",
                description = "The track's title to search for."
            ),
            CommandOptionKt(
                name = "author",
                description = "The track's author to search for."
            ),
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val scheduler = RobertifyAudioManagerKt[guild].scheduler
        val queueHandler = scheduler.queueHandler

        if (queueHandler.isEmpty)
            return event.replyEmbed(guild, GeneralMessages.NOTHING_IN_QUEUE).queue()

        val titleQuery = event.getRequiredOption("title").asString
        val authorQuery = event.getRequiredOption("author").asString
        val result = queueHandler.contents.firstOrNull {
            it.title.lowercase().startsWith(titleQuery.lowercase().trim()) && it.author.lowercase()
                .contains(authorQuery.lowercase().trim())
        } ?: return event.replyEmbed(
            guild,
            SearchQueueMessages.QUEUE_SEARCH_NOTHING_FOUND,
            Pair("{title}", titleQuery),
            Pair("{author}", authorQuery)
        ).queue()

        val embedBuilder = RobertifyEmbedUtilsKt.embedMessage(
            guild,
            SearchQueueMessages.QUEUE_SEARCH_ITEM_FOUND,
            Pair("{title}", result.title),
            Pair("{author}", result.author),
            Pair("{duration}", GeneralUtilsKt.formatTime(result.duration)),
            Pair("{position}", (queueHandler.contents.indexOf(result) + 1).toString()),
            Pair(
                "{requester}",
                scheduler.findRequester(result.identifier)?.toString()
                    ?: LocaleManagerKt[guild][GeneralMessages.UNKNOWN_REQUESTER]
            )
        )

        if (result is MirroringAudioTrack)
            embedBuilder.setThumbnail(result.artworkURL)

        event.replyEmbed { embedBuilder.build() }.queue()
    }
}