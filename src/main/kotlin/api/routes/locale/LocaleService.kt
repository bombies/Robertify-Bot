package api.routes.locale

import api.models.response.GenericResponse
import api.models.response.OkResponse
import api.models.service.AbstractGuildService
import main.commands.slashcommands.management.LanguageCommand
import net.dv8tion.jda.api.sharding.ShardManager

class LocaleService(shardManager: ShardManager) : AbstractGuildService(shardManager) {

    suspend fun setLocale(dto: LocaleDto): GenericResponse {
        val guild = guildUtils.getGuild(dto.server_id) ?: return noGuild(dto.server_id)
        LanguageCommand().setLocale(guild, dto.locale, shardManager)
        return OkResponse("You have set the locale in ${guild.name} to ${dto.locale.uppercase()}")
    }

}