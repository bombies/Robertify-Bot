package api.routes.themes

import api.models.response.ExceptionResponse
import api.models.response.GenericResponse
import api.models.response.OkResponse
import api.routes.themes.dto.ThemeDto
import api.utils.GuildUtils
import io.ktor.http.*
import main.commands.slashcommands.management.ThemeCommandKt
import main.constants.RobertifyThemeKt
import net.dv8tion.jda.api.sharding.ShardManager

class ThemesService(private val shardManager: ShardManager) {

    private val guildUtils = GuildUtils(shardManager)

    suspend fun updateTheme(themeDto: ThemeDto): GenericResponse {
        val guild = guildUtils.getGuild(themeDto.server_id) ?: return ExceptionResponse(
            reason = "There was no guild with id: ${themeDto.server_id}",
            status = HttpStatusCode.NotFound
        )

        ThemeCommandKt().updateTheme(guild, RobertifyThemeKt.parse(themeDto.theme), shardManager)
        return OkResponse(
            message = "Successfully set the theme for ${guild.name} to ${themeDto.theme.uppercase()}"
        )
    }

}