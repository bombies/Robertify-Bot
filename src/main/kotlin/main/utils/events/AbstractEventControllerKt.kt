package main.utils.events

import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.events.listener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.sharding.ShardManager

abstract class AbstractEventControllerKt {

    lateinit var shardManager: ShardManager

    fun register(shardManager: ShardManager) {
        this.shardManager = shardManager
        eventHandlerInvokers()
    }

    protected inline fun <reified T : GenericEvent> onEvent(
        crossinline handler: suspend CoroutineEventListener.(event: T) -> Unit
    ) =
        shardManager.listener<T> { handler(it) }

    /**
     * This function will be used to handle all the invocations
     * for functions that will be used to handle events.
     */
    abstract fun eventHandlerInvokers()
}