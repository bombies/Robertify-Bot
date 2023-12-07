package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.*
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.SearchQueueMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class SearchQueueCommand : AbstractSlashCommand(
    SlashCommand(
        name = "searchqueue",
        description = "Search your queue for a specific track and all of the current information.",
        options = listOf(
            CommandOption(
                name = "title",
                description = "The track's title to search for."
            ),
            CommandOption(
                name = "author",
                description = "The track's author to search for."
            ),
        )
    )
) {

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val scheduler = RobertifyAudioManager[guild].scheduler
        val queueHandler = scheduler.queueHandler

        if (queueHandler.isEmpty)
            return event.replyEmbed(GeneralMessages.NOTHING_IN_QUEUE).queue()

        val titleQuery = event.getRequiredOption("title").asString
        val authorQuery = event.getRequiredOption("author").asString
        val result = queueHandler.contents.firstOrNull {
            it.title.lowercase().startsWith(titleQuery.lowercase().trim()) && it.author.lowercase()
                .contains(authorQuery.lowercase().trim())
        } ?: return event.replyEmbed(
            SearchQueueMessages.QUEUE_SEARCH_NOTHING_FOUND,
            Pair("{title}", titleQuery),
            Pair("{author}", authorQuery)
        ).queue()

        val embedBuilder = RobertifyEmbedUtils.embedMessage(
            guild,
            SearchQueueMessages.QUEUE_SEARCH_ITEM_FOUND,
            Pair("{title}", result.title),
            Pair("{author}", result.author),
            Pair("{duration}", GeneralUtils.formatTime(result.length)),
            Pair("{position}", (queueHandler.contents.indexOf(result) + 1).toString()),
            Pair(
                "{requester}",
                scheduler.findRequester(result.identifier)?.toMention()
                    ?: LocaleManager[guild].getMessage(GeneralMessages.UNKNOWN_REQUESTER)
            )
        )

        if (result.artworkUrl != null)
            embedBuilder.setThumbnail(result.artworkUrl)

        event.replyEmbed { embedBuilder.build() }.queue()
    }
}