package main.events

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.management.requestchannel.RequestChannelEvents
import main.commands.slashcommands.misc.polls.PollEvents
import main.commands.slashcommands.util.suggestions.SuggestionChannelEvents
import main.main.Listener
import main.utils.pagination.PaginationEvents

object EventManager {

    private val logger by SLF4J

    /**
     * Instanciate each event
     */
    val registeredEvents by lazy {
        listOf(
            Listener(),
            PaginationEvents(),
            RequestChannelEvents(),
            SuggestionChannelEvents(),
            PollEvents()
        )
    }
}