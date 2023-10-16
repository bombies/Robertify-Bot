package main.events

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.management.requestchannel.RequestChannelEvents
import main.commands.slashcommands.misc.polls.PollEvents
import main.commands.slashcommands.util.suggestions.SuggestionChannelEvents
import main.main.Listener
import main.utils.pagination.events.PaginationButtonEvent
import main.utils.pagination.events.PaginationQueueButtonEvent

object EventManager {

    private val logger by SLF4J

    /**
     * Instantiate each event
     */
    val registeredEvents = listOf(
        Listener(),
        PaginationButtonEvent(),
        PaginationQueueButtonEvent(),
        RequestChannelEvents(),
        SuggestionChannelEvents(),
        PollEvents(),
        VoiceChannelEvents()
    )
}