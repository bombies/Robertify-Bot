package main.main

import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.listener
import main.utils.database.mongodb.cache.BotDBCacheKt
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory

class ListenerKt(private val shardManager: ShardManager) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    init {
        // Register listeners
        readyListener()
    }

    private fun readyListener() =
        shardManager.listener<ReadyEvent> { event ->
            val jda = event.jda
            logger.info("Watching ${event.guildAvailableCount} guilds on shard #${jda.shardInfo.shardId} (${event.guildUnavailableCount} unavailable)")
            BotDBCacheKt.instance!!.lastStartup = System.currentTimeMillis()
            RobertifyKt.shardManager.setPresence(OnlineStatus.ONLINE, Activity.listening("/help"))
        }

}