package main.events

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.management.requestchannel.RequestChannelEventsKt
import main.commands.slashcommands.misc.polls.PollEventsKt
import main.commands.slashcommands.util.suggestions.SuggestionChannelEventsKt
import main.main.ListenerKt
import main.utils.pagination.PaginationEventsKt

object EventManager {

    private val logger by SLF4J

    /**
     * Instanciate each event
     */
    val registeredEvents by lazy {
        listOf(
            ListenerKt(),
            PaginationEventsKt(),
            RequestChannelEventsKt(),
            SuggestionChannelEventsKt(),
            PollEventsKt()
        )
    }
}