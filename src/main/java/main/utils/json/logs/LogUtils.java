package main.utils.json.logs;

import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class LogUtils {
    private final LogConfig config;

    public LogUtils() {
        config = new LogConfig();
    }

    public void sendLog(Guild guild, LogType type, String message) {
        if (!config.channelIsSet(guild.getIdLong()))
            return;

        if (!new TogglesConfig().getLogToggle(guild, type))
            return;

        TextChannel channel = config.getChannel(guild.getIdLong());

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle(type.getEmoji().getAsMention() + " " + type.getTitle())
                        .setColor(type.getColor())
                        .setDescription(message)
                        .setTimestamp(Instant.now())
                        .build()
                ).queue();
    }

    public void createChannel(Guild guild) {
        if (config.channelIsSet(guild.getIdLong()))
            throw new IllegalArgumentException("This guild already has a log channel setup!");

        guild.createTextChannel("robertify-logs")
                .addPermissionOverride(guild.getPublicRole(), Collections.emptyList(), List.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(guild.getSelfMember(), List.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_WRITE), Collections.emptyList())
                .queue(channel -> config.setChannel(guild.getIdLong(), channel.getIdLong()));
    }
}
