package main.events;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.LofiCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
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
        if (event.getMember().equals(event.getGuild().getSelfMember())) {
            GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
            musicManager.scheduler.repeating = false;
            musicManager.scheduler.playlistRepeating = false;

            if (musicManager.audioPlayer.isPaused())
                musicManager.audioPlayer.setPaused(false);

            if (musicManager.audioPlayer.getPlayingTrack() != null)
                musicManager.audioPlayer.stopTrack();

            musicManager.scheduler.queue.clear();

            if (new DedicatedChannelConfig().isChannelSet(event.getGuild().getIdLong()))
                new DedicatedChannelConfig().updateMessage(event.getGuild());

            LofiCommand.getLofiEnabledGuilds().remove(event.getGuild().getIdLong());
        } else {
             VoiceChannel channelLeft = event.getChannelLeft();
             GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

             if (!selfVoiceState.inVoiceChannel()) return;

             if (!selfVoiceState.getChannel().equals(channelLeft)) return;

             doAutoLeave(event, channelLeft);
        }
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState voiceState = self.getVoiceState();

        if (!voiceState.inVoiceChannel()) return;


        if (event.getMember().getIdLong() == self.getIdLong()) {
            doAutoLeave(event, event.getChannelLeft());
        } else if (event.getChannelJoined().equals(voiceState.getChannel())) {
            resumeSong(event);
        } else if (voiceState.getChannel().equals(event.getChannelLeft())) {
            doAutoLeave(event, event.getChannelLeft());
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
            GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
            musicManager.audioPlayer.setPaused(true);
        }
    }

    void resumeSong(GenericGuildVoiceUpdateEvent event) {
        GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        if (musicManager.audioPlayer.isPaused() && event.getChannelJoined().getIdLong() == event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong()
             && !musicManager.isForcePaused())
            musicManager.audioPlayer.setPaused(false);
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
                    (e) -> {},
                    1L, TimeUnit.MINUTES,
                    () -> {
                        event.getGuild().getAudioManager().closeAudioConnection();
                        LofiCommand.getLofiEnabledGuilds().remove(event.getGuild().getIdLong());
                    }
            );
        }
    }
}
