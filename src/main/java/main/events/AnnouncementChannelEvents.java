package main.events;

import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class AnnouncementChannelEvents extends ListenerAdapter {

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        final var config = new GuildConfig();
        final var guild = event.getGuild();

        if (!config.announcementChannelIsSet(guild.getIdLong())) return;

        if (config.getAnnouncementChannelID(guild.getIdLong()) == event.getChannel().getIdLong())
            config.setAnnouncementChannelID(guild.getIdLong(), -1);
    }
}
