package api.routes.requestchannel

import api.models.response.ExceptionResponse
import api.models.response.GenericResponse
import api.models.response.OkResponse
import api.models.service.AbstractGuildService
import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonsDto
import api.routes.requestchannel.responses.RequestChannelCreationResponse
import api.utils.GuildUtils
import io.ktor.http.*
import io.ktor.util.logging.*
import main.commands.slashcommands.management.requestchannel.RequestChannelEditCommandKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager

class RequestChannelService(shardManager: ShardManager) : AbstractGuildService(shardManager) {

    companion object {
        private val logger = KtorSimpleLogger("RequestChannelService")
    }

    suspend fun createChannel(createChannelDto: CreateRequestChannelDto): GenericResponse {
        val guild = guildUtils.getGuild(createChannelDto.server_id) ?: return noGuild(createChannelDto.server_id)

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

    suspend fun toggleButton(dto: ToggleRequestChannelButtonDto): GenericResponse {
        val guild = guildUtils.getGuild(dto.server_id) ?: return noGuild(dto.server_id)

        val config = RequestChannelConfigKt(guild, shardManager)
        if (!config.isChannelSet())
            return ExceptionResponse(
                reason = "The request channel is hasn't been setup for ${guild.name}",
                status = HttpStatusCode.BadRequest
            )

        RequestChannelEditCommandKt().handleChannelButtonToggle(guild, dto.button.lowercase(), shardManager = shardManager)
        return OkResponse("Successfully toggled the ${dto.button} button in ${guild.name}")
    }

    suspend fun toggleButtons(dto: ToggleRequestChannelButtonsDto): GenericResponse {
        val guild = guildUtils.getGuild(dto.server_id) ?: return ExceptionResponse(
            reason = "There was no guild with id: ${dto.server_id}",
            status = HttpStatusCode.NotFound
        )

        val config = RequestChannelConfigKt(guild, shardManager)
        if (!config.isChannelSet())
            return ExceptionResponse(
                reason = "The request channel is hasn't been setup for ${guild.name}",
                status = HttpStatusCode.BadRequest
            )

        dto.buttons.forEach { button ->
            RequestChannelEditCommandKt().handleChannelButtonToggle(guild, button.lowercase(), shardManager = shardManager)
        }

        return OkResponse("Successfully toggled the ${dto.buttons.joinToString(", ")} buttons in ${guild.name}")
    }

    suspend fun deleteChannel(id: String): GenericResponse {
        val guild = guildUtils.getGuild(id) ?: return noGuild(id)

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