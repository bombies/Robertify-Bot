package main.constants

import main.main.ConfigKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed


enum class BotConstantsKt(private val str: String) {
    ICON_URL(ConfigKt.ICON_URL),
    ROBERTIFY_LOGO("https://i.imgur.com/KioK108.png"),
    ROBERTIFY_EMBED_TITLE(ConfigKt.BOT_NAME),
    SUPPORT_SERVER(ConfigKt.SUPPORT_SERVER),
    DEFAULT_IMAGE("https://i.imgur.com/VNQvjve.png"),
    USER_AGENT("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");

    override fun toString(): String {
        return str
    }

    companion object {
        fun getInsufficientPermsMessage(guild: Guild?, vararg permsNeeded: PermissionKt?): String {
            return LocaleManagerKt.getLocaleManager(guild).getMessage(
                GeneralMessages.INSUFFICIENT_PERMS,
                Pair("{permissions}", permsNeeded.mapNotNull { it?.name }.joinToString(", "))
            )
        }

        fun getUnexpectedErrorEmbed(guild: Guild?): MessageEmbed {
            return RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR)
                .build()
        }
    }
}
