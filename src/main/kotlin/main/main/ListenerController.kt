package main.main

import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.events.listener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.sharding.ShardManager

abstract class ListenerController protected constructor(protected val shardManager: ShardManager) {

    protected inline fun<reified T : GenericEvent> onEvent(crossinline handler: suspend CoroutineEventListener.(event: T) -> Unit) =
        shardManager.listener<T> { handler(it) }

}