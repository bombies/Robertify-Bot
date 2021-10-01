package main.events;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class VoiceChannelEvents extends ListenerAdapter {

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        if (event.getMember().equals(event.getGuild().getSelfMember()))
            event.getGuild().getSelfMember().deafen(true).queue();
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        if (event.getMember().equals(event.getGuild().getSelfMember())) {
            GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
//        musicManager.scheduler.repeating = false;
            musicManager.audioPlayer.stopTrack();
            musicManager.scheduler.queue.clear();
        }
    }
}
