package main.events;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.slashcommands.commands.audio.LofiCommand;
import main.commands.prefixcommands.audio.SkipCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
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

            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
                new DedicatedChannelConfig().updateMessage(guild);

            LofiCommand.getLofiEnabledGuilds().remove(guild.getIdLong());

            SkipCommand.clearVoteSkipInfo(guild);
        } else {
             VoiceChannel channelLeft = event.getChannelLeft();
             GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();

             if (!selfVoiceState.inVoiceChannel()) return;

             if (!selfVoiceState.getChannel().equals(channelLeft)) return;

             if (!new GuildConfig().get247(guild.getIdLong()))
                doAutoLeave(event, channelLeft);
        }
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        Guild guild = event.getGuild();
        Member self = guild.getSelfMember();
        GuildVoiceState voiceState = self.getVoiceState();

        if (!voiceState.inVoiceChannel()) return;

        VoiceChannel channelLeft = event.getChannelLeft();
        if (event.getMember().getIdLong() == self.getIdLong() && !new GuildConfig().get247(guild.getIdLong())) {
            doAutoLeave(event, channelLeft);
        } else if (event.getChannelJoined().equals(voiceState.getChannel())) {
            resumeSong(event);
        } else if (voiceState.getChannel().equals(channelLeft) && !new GuildConfig().get247(guild.getIdLong())) {
            doAutoLeave(event, channelLeft);
        }
    }

    void pauseSong(GenericGuildVoiceUpdateEvent event) {
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState voiceState = self.getVoiceState();

        if (voiceState == null) return;

        if (!voiceState.inVoiceChannel()) return;

        VoiceChannel channel = event.getChannelLeft();

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

    void doAutoLeave(GenericGuildVoiceUpdateEvent event, VoiceChannel channelLeft) {
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
                        if (!new GuildConfig().get247(event.getGuild().getIdLong()))
                            RobertifyAudioManager.getInstance().getMusicManager(event.getGuild()).leave();
                    }
            );
        }
    }
}
