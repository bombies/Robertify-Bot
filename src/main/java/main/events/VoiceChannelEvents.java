package main.events;

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

        if (channelJoined != null && channelLeft != null) {
            Member self = guild.getSelfMember();
            GuildVoiceState voiceState = self.getVoiceState();

            if (!voiceState.inAudioChannel()) return;

            final var guildConfig = new GuildConfig(guild);

            if (event.getMember().getIdLong() == self.getIdLong() && !guildConfig.get247()) {
                doAutoLeave(event, channelLeft);
            } else if (event.getChannelJoined().equals(voiceState.getChannel())) {
                resumeSong(event);
            } else if (voiceState.getChannel().equals(channelLeft) && !guildConfig.get247()) {
                doAutoLeave(event, channelLeft);
            }
        } else if (channelLeft == null) {
            resumeSong(event);
        } else {
            if (event.getMember().equals(guild.getSelfMember())) {
                final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
                musicManager.getScheduler().setRepeating(false);
                musicManager.getScheduler().setPlaylistRepeating(false);

                if (musicManager.getPlayer().isPaused())
                    musicManager.getPlayer().setPaused(false);

                if (musicManager.getPlayer().getPlayingTrack() != null)
                    musicManager.getPlayer().stopTrack();

                musicManager.getScheduler().getQueue().clear();
                musicManager.getPlayer().getFilters().clear().commit();

                final var dedicatedChannelConfig = new RequestChannelConfig(guild);
                if (dedicatedChannelConfig.isChannelSet())
                    dedicatedChannelConfig.updateMessage();

                SkipCommand.clearVoteSkipInfo(guild);
            } else {
                final var selfVoiceState = guild.getSelfMember().getVoiceState();

                if (!selfVoiceState.inAudioChannel()) return;

                if (!selfVoiceState.getChannel().equals(channelLeft)) return;

                if (!new GuildConfig(guild).get247())
                    doAutoLeave(event, channelLeft);
            }
        }
    }

    void pauseSong(GuildVoiceUpdateEvent event) {
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState voiceState = self.getVoiceState();

        if (voiceState == null) return;

        if (!voiceState.inAudioChannel()) return;

        final var channel = event.getChannelLeft();

        if (!channel.equals(voiceState.getChannel())) return;

        if (channel.getMembers().size() == 1) {
            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
            musicManager.getPlayer().setPaused(true);
        }
    }

    void resumeSong(GuildVoiceUpdateEvent event) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        if (musicManager.getPlayer().isPaused() && event.getChannelJoined().getIdLong() == event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong()
             && !musicManager.isForcePaused())
            musicManager.getPlayer().setPaused(false);
    }

    void doAutoLeave(GuildVoiceUpdateEvent event, AudioChannel channelLeft) {
        if (channelLeft.getMembers().size() == 1) {
            pauseSong(event);
            waiter.waitForEvent(
                    GuildVoiceUpdateEvent.class,
                    (e) -> {
                        // If it's a leave event
                        if (e.getChannelJoined() == null)
                            return false;

                        final var channelJoined = e.getChannelJoined();

                        // If it's a move event
                        if (event.getChannelLeft() != null)
                            return e.getMember().getIdLong() == event.getGuild().getSelfMember().getIdLong() ?
                                    channelJoined.getMembers().size() > 1 : channelJoined.equals(channelLeft);
                        // If it's a join event
                        else
                            return channelJoined.equals(channelLeft);
                    },
                    (e) -> {
                        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
                        if (musicManager.getPlayer().isPaused() && !musicManager.isForcePaused())
                            musicManager.getPlayer().setPaused(false);
                    },
                    1L, TimeUnit.MINUTES,
                    () -> {
                        if (!new GuildConfig(event.getGuild()).get247())
                            RobertifyAudioManager.getInstance().getMusicManager(event.getGuild()).leave();
                    }
            );
        }
    }
}
