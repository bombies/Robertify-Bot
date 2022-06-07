package main.utils.json.logs;

import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.LocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class LogUtils {
    private final LogConfig config;
    private final Guild guild;

    public LogUtils(Guild guild) {
        config = new LogConfig(guild);
        this.guild = guild;
    }

    public void sendLog(LogType type, String message) {
        if (!config.channelIsSet())
            return;

        if (!new TogglesConfig(guild).getLogToggle(type))
            return;

        TextChannel channel = config.getChannel();

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle(type.getEmoji().getAsMention() + " " + type.getTitle())
                        .setColor(type.getColor())
                        .setDescription(message)
                        .setTimestamp(Instant.now())
                        .build()
                ).queue();
    }

    public void sendLog(LogType type, LocaleMessage message) {
        if (!config.channelIsSet())
            return;

        if (!new TogglesConfig(guild).getLogToggle(type))
            return;

        final var localeManager = LocaleManager.getLocaleManager(guild);
        TextChannel channel = config.getChannel();

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle(type.getEmoji().getAsMention() + " " + type.getTitle())
                        .setColor(type.getColor())
                        .setDescription(localeManager.getMessage(message))
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }

    @SafeVarargs
    public final void sendLog(LogType type, LocaleMessage message, Pair<String, String>... placeholders) {
        if (!config.channelIsSet())
            return;

        if (!new TogglesConfig(guild).getLogToggle(type))
            return;

        final var localeManager = LocaleManager.getLocaleManager(guild);
        TextChannel channel = config.getChannel();

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle(type.getEmoji().getAsMention() + " " + type.getTitle())
                        .setColor(type.getColor())
                        .setDescription(localeManager.getMessage(message, placeholders))
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }

    public void createChannel() {
        if (config.channelIsSet())
            config.removeChannel();

        guild.createTextChannel("robertify-logs")
                .addPermissionOverride(guild.getPublicRole(), Collections.emptyList(), List.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(guild.getSelfMember(), List.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_WRITE), Collections.emptyList())
                .queue(channel -> config.setChannel(channel.getIdLong()));
    }
}
