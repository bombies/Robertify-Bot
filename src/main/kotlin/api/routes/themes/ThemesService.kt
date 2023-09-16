package api.routes.themes

import api.models.response.GenericResponse
import api.models.response.OkResponse
import api.models.service.AbstractGuildService
import api.routes.themes.dto.ThemeDto
import main.commands.slashcommands.management.ThemeCommand
import main.constants.RobertifyTheme
import net.dv8tion.jda.api.sharding.ShardManager

class ThemesService(shardManager: ShardManager) : AbstractGuildService(shardManager) {

    suspend fun updateTheme(themeDto: ThemeDto): GenericResponse {
        val guild = guildUtils.getGuild(themeDto.server_id) ?: return noGuild(themeDto.server_id)

        ThemeCommand().updateTheme(guild, RobertifyTheme.parse(themeDto.theme), shardManager)
        return OkResponse(
            message = "Successfully set the theme for ${guild.name} to ${themeDto.theme.uppercase()}"
        )
    }

}