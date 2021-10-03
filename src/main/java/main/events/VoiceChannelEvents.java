package main.events;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class VoiceChannelEvents extends ListenerAdapter {

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
//        resumeSong(event);
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        if (event.getMember().equals(event.getGuild().getSelfMember())) {
            GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
//        musicManager.scheduler.repeating = false;
            musicManager.audioPlayer.stopTrack();
            musicManager.scheduler.queue.clear();
        } else {
//            pauseSong(event);
        }
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState voiceState = self.getVoiceState();

        if (event.getChannelJoined().equals(voiceState.getChannel())) {
//            resumeSong(event);
        } else {
//            pauseSong(event);
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
            GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            musicManager.audioPlayer.setPaused(true);
        }
    }

    void resumeSong(GenericGuildVoiceUpdateEvent event) {
        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        if (musicManager.audioPlayer.isPaused())
            musicManager.audioPlayer.setPaused(false);
    }
}
