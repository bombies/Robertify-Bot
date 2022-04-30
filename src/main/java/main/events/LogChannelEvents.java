package main.events;

import main.utils.json.logs.LogConfig;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class LogChannelEvents extends ListenerAdapter {

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (!event.isFromType(ChannelType.TEXT)) return;

        LogConfig logConfig = new LogConfig();

        if (!logConfig.channelIsSet(event.getGuild().getIdLong()))
            return;

        if (event.getChannel().getIdLong() == logConfig.getChannelID(event.getGuild().getIdLong()))
            logConfig.removeChannel(event.getGuild().getIdLong());
    }
}
