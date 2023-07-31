package main.utils.api.robertify.models

import main.constants.RobertifyTheme
import main.utils.json.guildconfig.GuildConfig
import net.dv8tion.jda.api.entities.Guild

data class RobertifyPremium(
    val userId: Long,
    val email: String,
    val type: Int,
    val tier: Int,
    val servers: List<String>,
    val startedAt: Long,
    val endsAt: Long,
) {

    companion object {
        fun resetPremiumFeatures(guild: Guild) {
            GuildConfig(guild)
                .setManyFields {
                    setAutoPlay(false)
                        .set247(false)
                        .setTheme(RobertifyTheme.GREEN)
                }
        }
    }


}