package main.events;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.SkipCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class VoiceChannelEvents extends ListenerAdapter {
    public static final EventWaiter waiter = new EventWaiter();

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        resumeSong(event);
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        Guild guild = event.getGuild();
        if (event.getMember().equals(guild.getSelfMember())) {
            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
            musicManager.getScheduler().repeating = false;
            musicManager.getScheduler().playlistRepeating = false;

            if (musicManager.getPlayer().isPaused())
                musicManager.getPlayer().setPaused(false);

            if (musicManager.getPlayer().getPlayingTrack() != null)
                musicManager.getPlayer().stopTrack();

            musicManager.getScheduler().queue.clear();
            musicManager.getPlayer().getFilters().clear().commit();

            final var dedicatedChannelConfig = new DedicatedChannelConfig(guild);
            if (dedicatedChannelConfig.isChannelSet())
                dedicatedChannelConfig.updateMessage();

            SkipCommand.clearVoteSkipInfo(guild);
        } else {
             final var channelLeft = event.getChannelLeft();
             final var selfVoiceState = guild.getSelfMember().getVoiceState();

             if (!selfVoiceState.inAudioChannel()) return;

             if (!selfVoiceState.getChannel().equals(channelLeft)) return;

             if (!new GuildConfig(guild).get247())
                doAutoLeave(event, channelLeft);
        }
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        Guild guild = event.getGuild();
        Member self = guild.getSelfMember();
        GuildVoiceState voiceState = self.getVoiceState();

        if (!voiceState.inAudioChannel()) return;

        final var channelLeft = event.getChannelLeft();
        final var guildConfig = new GuildConfig(guild);

        if (event.getMember().getIdLong() == self.getIdLong() && !guildConfig.get247()) {
            doAutoLeave(event, channelLeft);
        } else if (event.getChannelJoined().equals(voiceState.getChannel())) {
            resumeSong(event);
        } else if (voiceState.getChannel().equals(channelLeft) && !guildConfig.get247()) {
            doAutoLeave(event, channelLeft);
        }
    }

    void pauseSong(GenericGuildVoiceUpdateEvent event) {
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

    void resumeSong(GenericGuildVoiceUpdateEvent event) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        if (musicManager.getPlayer().isPaused() && event.getChannelJoined().getIdLong() == event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong()
             && !musicManager.isForcePaused())
            musicManager.getPlayer().setPaused(false);
    }

    void doAutoLeave(GenericGuildVoiceUpdateEvent event, AudioChannel channelLeft) {
        if (channelLeft.getMembers().size() == 1) {
            pauseSong(event);
            waiter.waitForEvent(
                    GenericGuildVoiceUpdateEvent.class,
                    (e) -> {
                        if (!(e instanceof GuildVoiceJoinEvent)
                            && !(e instanceof GuildVoiceMoveEvent)) return false;

                        if (e instanceof GuildVoiceMoveEvent moveEvent) {
                            final var channelJoined = moveEvent.getChannelJoined();

                            if (moveEvent.getMember().getIdLong() == event.getGuild().getSelfMember().getIdLong()) {
                                return channelJoined.getMembers().size() > 1;
                            } else {
                                return channelJoined.equals(channelLeft);
                            }
                        } else  {
                            final var channelJoined = e.getChannelJoined();
                            return channelJoined.equals(channelLeft);
                        }
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
