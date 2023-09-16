package api.models.service

import api.models.response.ExceptionResponse
import api.utils.GuildUtils
import io.ktor.http.*
import net.dv8tion.jda.api.sharding.ShardManager

abstract class AbstractGuildService(protected val shardManager: ShardManager) {

    protected fun noGuild(serverId: String) = ExceptionResponse(
        reason = "There was no guild with id: $serverId",
        status = HttpStatusCode.NotFound
    )

    protected val guildUtils = GuildUtils(shardManager)

}