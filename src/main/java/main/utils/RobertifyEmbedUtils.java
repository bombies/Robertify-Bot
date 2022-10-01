package main.utils;

import main.main.Robertify;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.LocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.HashMap;
import java.util.function.Supplier;

public class RobertifyEmbedUtils {
    private final static HashMap<Long, Supplier<EmbedBuilder>> guildEmbedSuppliers = new HashMap<>();

    public static void setEmbedBuilder(Guild guild, Supplier<EmbedBuilder> supplier) {
        guildEmbedSuppliers.put(guild.getIdLong(), supplier);
    }

    public static EmbedBuilder getEmbedBuilder(Guild guild) {
        try {
            return guildEmbedSuppliers.get(guild.getIdLong()).get();
        } catch (NullPointerException e) {
            GeneralUtils.setDefaultEmbed(guild);
            return guildEmbedSuppliers.get(guild.getIdLong()).get();
        }
    }

    public static EmbedBuilder embedMessage(Guild guild, String message) {
        return getDefaultEmbed(guild.getIdLong()).setDescription(message);
    }

    public static EmbedBuilder embedMessage(Guild guild, LocaleMessage message) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setDescription(localeManager.getMessage(message));
    }

    @SafeVarargs
    public static EmbedBuilder embedMessage(Guild guild, LocaleMessage message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setDescription(localeManager.getMessage(message, placeholders));
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, String title, String message) {
        return getDefaultEmbed(guild.getIdLong()).setTitle(title).setDescription(message);
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, LocaleMessage message) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setTitle(localeManager.getMessage(title)).setDescription(localeManager.getMessage(message));
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, String title, LocaleMessage message) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setTitle(title).setDescription(localeManager.getMessage(message));
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, String message) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setTitle(localeManager.getMessage(title)).setDescription(message);
    }

    @SafeVarargs
    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, LocaleMessage message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setTitle(localeManager.getMessage(title)).setDescription(localeManager.getMessage(message, placeholders));
    }

    @SafeVarargs
    public static EmbedBuilder embedMessageWithTitle(Guild guild, String title, LocaleMessage message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setTitle(title).setDescription(localeManager.getMessage(message, placeholders));
    }

    @SafeVarargs
    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, String message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild.getIdLong()).setTitle(localeManager.getMessage(title, placeholders)).setDescription(message);
    }

    public static boolean getEphemeralState(GuildMessageChannel channel) {
        final var dedicatedChannelConfig = new DedicatedChannelConfig(channel.getGuild());
        if (!dedicatedChannelConfig.isChannelSet())
            return false;
        return dedicatedChannelConfig.getChannelID() == channel.getIdLong();
    }

    public static boolean getEphemeralState(GuildMessageChannel channel, boolean _default) {
        final var dedicatedChannelConfig = new DedicatedChannelConfig(channel.getGuild());
        if (!dedicatedChannelConfig.isChannelSet())
            return _default;
        return dedicatedChannelConfig.getChannelID() == channel.getIdLong();
    }

    private static EmbedBuilder getDefaultEmbed(long gid) {
        try {
            return guildEmbedSuppliers.get(gid).get();
        } catch (NullPointerException e) {
            GeneralUtils.setDefaultEmbed(Robertify.getShardManager().getGuildById(gid));
            return guildEmbedSuppliers.get(gid).get();
        }
    }

}
