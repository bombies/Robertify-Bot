package main.events

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.management.requestchannel.RequestChannelEventsKt
import main.main.ListenerKt
import main.utils.pagination.PaginationEventsKt
import net.dv8tion.jda.api.sharding.ShardManager

object EventManager {

    private val logger by SLF4J

    val events = listOf(
        ListenerKt(),
        PaginationEventsKt(),
        RequestChannelEventsKt()
    )

    fun ShardManager.registerEvent(event: AbstractEventControllerKt) =
        event.register(this)

    fun ShardManager.registerEvents(events: List<AbstractEventControllerKt>) =
        events.forEach { it.register(this) }

}