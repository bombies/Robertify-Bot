package api.routes.requestchannel

import api.models.response.ExceptionResponse
import api.models.response.GenericResponse
import api.models.response.OkResponse
import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.routes.requestchannel.responses.RequestChannelCreationResponse
import api.utils.GuildUtils
import io.ktor.http.*
import io.ktor.util.logging.*
import main.commands.slashcommands.management.requestchannel.RequestChannelEditCommandKt
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager

class RequestChannelService(private val shardManager: ShardManager) {

    companion object {
        private val logger = KtorSimpleLogger("RequestChannelService")
    }


    private val guildUtils = GuildUtils(shardManager)

    suspend fun createChannel(createChannelDto: CreateRequestChannelDto): GenericResponse {
        val guild = guildUtils.getGuild(createChannelDto.server_id) ?: return ExceptionResponse(
            reason = "There was no guild with id: ${createChannelDto.server_id}",
            status = HttpStatusCode.NotFound
        )

        return try {
            val channel = RequestChannelEditCommandKt().createRequestChannel(guild, shardManager).await()
            OkResponse(
                message = "Success",
                data = RequestChannelCreationResponse(
                    channel_id = channel.channelId.toString(),
                    message_id = channel.messageId.toString()
                ).build()
            )
        } catch (e: InsufficientPermissionException) {
            ExceptionResponse(
                reason = "I don't have the required permissions to create a request channel",
                status = HttpStatusCode.Forbidden
            )
        }
    }

    suspend fun deleteChannel(id: String): GenericResponse {
        val guild = guildUtils.getGuild(id) ?: return ExceptionResponse(
            reason = "There was no guild with id: $id",
            status = HttpStatusCode.NotFound
        )

        return try {
            RequestChannelEditCommandKt().deleteRequestChannel(guild, shardManager)
            OkResponse(message = "Successfully deleted request channel for ${guild.name}!")
        } catch (e: InsufficientPermissionException) {
            ExceptionResponse(
                reason = "I don't have the required permissions to delete create a request channel",
                status = HttpStatusCode.Forbidden
            )
        } catch (e: IllegalArgumentException) {
            ExceptionResponse(
                reason = "${guild.name} doesn't have a request channel to delete!",
                status = HttpStatusCode.NotFound
            )
        }
    }

}