package main.constants

import main.main.Config
import main.utils.RobertifyEmbedUtilsKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed


object BotConstantsKt {
    val ICON_URL = Config.ICON_URL
    val ROBERTIFY_EMBED_TITLE = Config.BOT_NAME
    val SUPPORT_SERVER = Config.SUPPORT_SERVER
    const val ROBERTIFY_LOGO = "https://i.imgur.com/KioK108.png"
    const val DEFAULT_IMAGE = "https://i.imgur.com/VNQvjve.png"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36"

    fun getInsufficientPermsMessage(guild: Guild?, vararg permsNeeded: RobertifyPermissionKt?): String {
        return LocaleManagerKt[guild].getMessage(
            GeneralMessages.INSUFFICIENT_PERMS,
            Pair("{permissions}", permsNeeded.mapNotNull { it?.name }.joinToString(", "))
        )
    }

    fun getUnexpectedErrorEmbed(guild: Guild?): MessageEmbed {
        return RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR)
            .build()
    }
}
