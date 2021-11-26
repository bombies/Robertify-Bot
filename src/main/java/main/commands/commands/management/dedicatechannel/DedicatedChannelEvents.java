package main.commands.commands.management.dedicatechannel;

import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.main.Listener;
import main.utils.database.ServerUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class DedicatedChannelEvents extends ListenerAdapter {

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        if (!config.isChannelSet(guild.getId())) return;
        if (!config.getChannelID(guild.getId()).equals(event.getChannel().getId())) return;

        config.removeChannel(guild.getId());
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        if (!config.isChannelSet(guild.getId())) return;
        if (!config.getChannelID(guild.getId()).equals(event.getChannel().getId())) return;

        final String message = event.getMessage().getContentRaw();

        if (!message.startsWith(ServerUtils.getPrefix(guild.getIdLong())) && !event.getAuthor().isBot()) {
            var toggleConfig = new TogglesConfig();

            if (toggleConfig.getToggle(guild, Toggles.ANNOUNCE_MESSAGES))
                toggleConfig.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, false);

            PlayerManager.getInstance()
                    .loadAndPlay(event.getChannel(), "ytsearch:" + message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(),
                            new CommandContext(event, null));
        }

        if (event.getAuthor().isBot())
            event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
        else
            event.getMessage().delete().queue();
    }
}
