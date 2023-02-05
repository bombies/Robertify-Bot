package main.utils;

import main.main.Robertify;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.LocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.function.Supplier;

public class RobertifyEmbedUtils {
    private final static HashMap<Long, Supplier<EmbedBuilder>> guildEmbedSuppliers = new HashMap<>();

    public static void setEmbedBuilder(Guild guild, Supplier<EmbedBuilder> supplier) {
        guildEmbedSuppliers.put(guild.getIdLong(), supplier);
    }

    public static void setEmbedBuilder(Supplier<EmbedBuilder> supplier) {
        guildEmbedSuppliers.put(0L, supplier);
    }

    public static EmbedBuilder getEmbedBuilder(Guild guild) {
        if (guild == null)
            return getDefaultEmbed();

        try {
            return guildEmbedSuppliers.get(guild.getIdLong()).get();
        } catch (NullPointerException e) {
            GeneralUtils.setDefaultEmbed(guild);
            return guildEmbedSuppliers.get(guild.getIdLong()).get();
        }
    }

    public static EmbedBuilder embedMessage(@Nullable Guild guild, String message) {
        final var builder = guild == null ? getDefaultEmbed() : getDefaultEmbed(guild);
        return builder.setDescription(message);
    }

    public static EmbedBuilder embedMessage(@Nullable Guild guild, LocaleMessage message) {
        if (guild == null)
            return embedMessage(message);
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setDescription(localeManager.getMessage(message));
    }

    public static EmbedBuilder embedMessage(LocaleMessage message) {
        final var localeManager = LocaleManager.globalManager();
        return getDefaultEmbed().setDescription(localeManager.getMessage(message));
    }

    @SafeVarargs
    public static EmbedBuilder embedMessage(Guild guild, LocaleMessage message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setDescription(localeManager.getMessage(message, placeholders));
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, String title, String message) {
        return getDefaultEmbed(guild).setTitle(title).setDescription(message);
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, LocaleMessage message) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title)).setDescription(localeManager.getMessage(message));
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, String title, LocaleMessage message) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setTitle(title).setDescription(localeManager.getMessage(message));
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, String message) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title)).setDescription(message);
    }

    @SafeVarargs
    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, LocaleMessage message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title)).setDescription(localeManager.getMessage(message, placeholders));
    }

    @SafeVarargs
    public static EmbedBuilder embedMessageWithTitle(Guild guild, String title, LocaleMessage message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setTitle(title).setDescription(localeManager.getMessage(message, placeholders));
    }

    @SafeVarargs
    public static EmbedBuilder embedMessageWithTitle(Guild guild, LocaleMessage title, String message, Pair<String, String>... placeholders) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title, placeholders)).setDescription(message);
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

    private static EmbedBuilder getDefaultEmbed(@Nullable Guild guild) {
        if (guild == null)
            return getDefaultEmbed();

        try {
            return guildEmbedSuppliers.get(guild.getIdLong()).get();
        } catch (NullPointerException e) {
            GeneralUtils.setDefaultEmbed(Robertify.getShardManager().getGuildById(guild.getIdLong()));
            return guildEmbedSuppliers.get(guild.getIdLong()).get();
        }
    }

    private static EmbedBuilder getDefaultEmbed() {
        try {
            return guildEmbedSuppliers.get(0L).get();
        } catch (NullPointerException e) {
            GeneralUtils.setDefaultEmbed();
            return guildEmbedSuppliers.get(0L).get();
        }
    }

}
