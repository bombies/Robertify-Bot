package api.utils

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager

class GuildUtils(private val shardManager: ShardManager) {

    fun getGuild(id: String): Guild? =
        shardManager.getGuildById(id)

}
