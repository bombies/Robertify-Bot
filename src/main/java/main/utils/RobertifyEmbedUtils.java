package main.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;
import java.util.function.Supplier;

public class RobertifyEmbedUtils {
    private final static HashMap<Long, Supplier<EmbedBuilder>> guildEmbedSuppliers = new HashMap<>();

    public static void setEmbedBuilder(Guild guild, Supplier<EmbedBuilder> supplier) {
        guildEmbedSuppliers.put(guild.getIdLong(), supplier);
    }

    public static EmbedBuilder embedMessage(Guild guild, String message) {
        return getDefaultEmbed(guild.getIdLong()).setDescription(message);
    }

    public static EmbedBuilder embedMessageWithTitle(Guild guild, String title, String message) {
        return getDefaultEmbed(guild.getIdLong()).setTitle(title).setDescription(message);
    }

    private static EmbedBuilder getDefaultEmbed(long gid) {
        return guildEmbedSuppliers.get(gid).get();
    }

}
