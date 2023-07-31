package api.utils

import dev.minn.jda.ktx.events.await
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.sharding.ShardManager

class GuildUtils(private val shardManager: ShardManager) {

    suspend fun getGuild(id: String): Guild?  {
        if (shardManager.getStatus(0)!! != JDA.Status.CONNECTED)
            shardManager.await<ReadyEvent>()
        return shardManager.getGuildById(id)
    }

}
