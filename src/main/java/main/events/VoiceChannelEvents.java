package main.events;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.utils.database.sqlite3.BotDB;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
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

            musicManager.audioPlayer.stopTrack();
            musicManager.scheduler.queue.clear();

            if (new DedicatedChannelConfig().isChannelSet(event.getGuild().getId()))
                new DedicatedChannelConfig().updateMessage(event.getGuild());

        } else {
             VoiceChannel channelLeft = event.getChannelLeft();
             GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

             if (!selfVoiceState.inVoiceChannel()) return;

             if (!selfVoiceState.getChannel().equals(channelLeft)) return;

             if (channelLeft.getMembers().size() == 1) {
                 pauseSong(event);
                 TextChannel channel = event.getGuild().getTextChannelById(new BotDB().getAnnouncementChannel(event.getGuild().getIdLong()));
                 channel.sendMessageEmbeds(EmbedUtils.embedMessage("Everyone's left me alone! ☹️" +
                         "\nI will disconnect from "+channelLeft.getAsMention()+" in 1 minute.").build())
                                 .queue();
                 waiter.waitForEvent(
                        GuildVoiceJoinEvent.class,
                        (e) -> e.getChannelJoined().equals(channelLeft),
                        (e) -> {
                            channel.sendMessageEmbeds(EmbedUtils.embedMessage("Someone joined me! 🥳" +
                                    "\nNow resuming the music!").build())
                                    .queue();
                        },
                        1L, TimeUnit.MINUTES,
                        () -> {
                            event.getGuild().getAudioManager().closeAudioConnection();
                            channel.sendMessageEmbeds(EmbedUtils.embedMessage("I've disconnected from " + channelLeft.getAsMention())
                                    .build())
                                    .queue();
                        }
                );
             }
        }
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState voiceState = self.getVoiceState();

        if (event.getChannelJoined().equals(voiceState.getChannel())) {
            resumeSong(event);
        } else {
            pauseSong(event);
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
}
