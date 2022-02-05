package main.events;

import main.utils.json.logs.LogConfig;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class LogChannelEvents extends ListenerAdapter {

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        LogConfig logConfig = new LogConfig();

        if (!logConfig.channelIsSet(event.getGuild().getIdLong()))
            return;

        if (event.getChannel().getIdLong() == logConfig.getChannelID(event.getGuild().getIdLong()))
            logConfig.removeChannel(event.getGuild().getIdLong());
    }
}
