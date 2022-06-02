package main.constants;

import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public enum BotConstants {
    ICON_URL(Config.get(ENV.ICON_URL)),
    ROBERTIFY_LOGO("https://i.imgur.com/KioK108.png"),
    ROBERTIFY_EMBED_TITLE(Config.get(ENV.BOT_NAME)),
    SUPPORT_SERVER(Config.get(ENV.BOT_SUPPORT_SERVER)),
    DEFAULT_IMAGE("https://i.imgur.com/VNQvjve.png"),
    USER_AGENT("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");

    private final String str;

    BotConstants(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }

    public static String getInsufficientPermsMessage(Guild guild, Permission... permsNeeded) {
        return LocaleManager.getLocaleManager(guild).getMessage(RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", GeneralUtils.arrayToString(permsNeeded)));
    }

    public static MessageEmbed getUnexpectedErrorEmbed(Guild guild) {
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR)
                .build();
    }
}
