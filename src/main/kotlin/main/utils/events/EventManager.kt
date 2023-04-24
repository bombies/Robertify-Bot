package main.utils.events

import main.main.ListenerKt
import main.utils.pagination.PaginationEventsKt
import net.dv8tion.jda.api.sharding.ShardManager

object EventManager {
    private val events = mutableListOf<AbstractEventControllerKt>()

    fun ShardManager.registerEvent(event: AbstractEventControllerKt) =
        event.register(this)

    fun ShardManager.registerEvents(events: List<AbstractEventControllerKt>) =
        events.forEach { it.register(this) }

    init {
        addEvents(
            ListenerKt(),
            PaginationEventsKt()
        )
    }

    private fun addEvents(vararg events: AbstractEventControllerKt) =
        this.events.addAll(events.toList())

    fun getRegisteredEvents(): List<AbstractEventControllerKt> =
        events.toList()
}