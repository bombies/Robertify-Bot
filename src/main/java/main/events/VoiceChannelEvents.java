package main.events;

import main.audiohandlers.DisconnectManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.SkipCommand;
import main.utils.EventWaiter;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class VoiceChannelEvents extends ListenerAdapter {
    public static final EventWaiter waiter = new EventWaiter();

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        final var guild = event.getGuild();
        final var channelLeft = event.getChannelLeft();
        final var channelJoined = event.getChannelJoined();
        final var self = guild.getSelfMember();
        final var selfVoiceState = self.getVoiceState();
        final var guildMusicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);

        /*
         * If the bot has left voice channels entirely
         */
        if (event.getMember().getId().equals(self.getId()) && (channelLeft != null && channelJoined == null)) {
            guildMusicManager.clear();
            return;
        }

        if (selfVoiceState == null || !selfVoiceState.inAudioChannel())
            return;

        final var guildConfig = new GuildConfig(guild);
        final var guildDisconnector = DisconnectManager.getInstance().getGuildDisconnector(guild);

        /*
         * If the user has left voice channels entirely or
         * switched and the channel left is empty we want
         * to disconnect the bot unless 24/7 mode is enabled.
         */
        if (
                ((channelJoined == null && channelLeft != null) || (channelJoined != null && channelLeft != null))
                        && channelLeft.getIdLong() == selfVoiceState.getChannel().getIdLong()
        ) {
            if (guildConfig.get247() || guildDisconnector.disconnectScheduled() || channelLeft.getMembers().size() > 1)
                return;
            guildMusicManager.getPlayer().setPaused(true);
            guildDisconnector.scheduleDisconnect(true);
        }

        /*
         * If the user is joining a voice channel for the
         * first time and the bot is awaiting disconnect,
         * cancel the disconnect and resume playing the song
         * if the song is paused.
         */
        else if (channelJoined != null && channelJoined.getIdLong() == selfVoiceState.getChannel().getIdLong()) {
            if (!guildDisconnector.disconnectScheduled())
                return;
            guildDisconnector.cancelDisconnect();
            guildMusicManager.getPlayer().setPaused(false);
        }
    }
}
