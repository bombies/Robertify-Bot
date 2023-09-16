package main.events

import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.events.listener
import main.main.Robertify
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

abstract class AbstractEventController : ListenerAdapter() {

    companion object {
        protected val shardManager = Robertify.shardManager
    }

    protected inline fun <reified T : GenericEvent> onEvent(
        crossinline handler: suspend CoroutineEventListener.(event: T) -> Unit
    ) =
        shardManager.listener<T> { handler(it) }
}