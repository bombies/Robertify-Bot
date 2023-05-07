package main.utils.api.robertify.models

import main.constants.RobertifyThemeKt
import main.utils.json.guildconfig.GuildConfigKt
import net.dv8tion.jda.api.entities.Guild

data class RobertifyPremiumKt(
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
            GuildConfigKt(guild)
                .setManyFields {
                    setAutoPlay(false)
                        .set247(false)
                        .setTheme(RobertifyThemeKt.GREEN)
                }
        }
    }


}