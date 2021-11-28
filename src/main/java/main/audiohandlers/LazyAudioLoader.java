package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.HashMap;
import java.util.List;

public class LazyAudioLoader implements AudioLoadResultHandler {
    private final Guild guild;
    private final User sender;
    private final Message msg;
    private final GuildMusicManager musicManager;
    private final boolean announceMsg;
    private final TextChannel channel;
    private final HashMap<AudioTrack, User> trackRequestedByUser;
    private final String trackUrl;
    private final SlashCommandEvent event;


    public LazyAudioLoader(User sender, GuildMusicManager musicManager, Message msg, TextChannel channel, HashMap<AudioTrack, User> trackRequestedByUser,
                           String trackUrl, boolean announceMsg) {
        this.sender = sender;
        this.guild = musicManager.scheduler.getGuild();
        this.musicManager = musicManager;
        this.msg = msg;
        this.channel = channel;
        this.trackRequestedByUser = trackRequestedByUser;
        this.trackUrl = trackUrl;
        this.announceMsg = announceMsg;
        this.event = null;
    }

    public LazyAudioLoader(User sender, GuildMusicManager musicManager, Message msg, TextChannel channel, HashMap<AudioTrack, User> trackRequestedByUser,
                           SlashCommandEvent event, String trackUrl, boolean announceMsg) {
        this.sender = sender;
        this.guild = musicManager.scheduler.getGuild();
        this.musicManager = musicManager;
        this.msg = msg;
        this.channel = channel;
        this.trackRequestedByUser = trackRequestedByUser;
        this.trackUrl = trackUrl;
        this.event = event;
        this.announceMsg = announceMsg;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        if (announceMsg) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + audioTrack.getInfo().title
                    + "` by `" + audioTrack.getInfo().author + "`");

            if (event == null)
                channel.sendMessageEmbeds(eb.build()).queue();
            else
                event.replyEmbeds(eb.build()).setEphemeral(false).queue();
        } else {
            TogglesConfig toggleConfig = new TogglesConfig();
            DedicatedChannelConfig config = new DedicatedChannelConfig();

            if (config.isChannelSet(guild.getId()))
                toggleConfig.setToggle(
                        guild, Toggles.ANNOUNCE_MESSAGES,
                        config.getOriginalAnnouncementToggle(guild.getId())
                );
        }

        trackRequestedByUser.put(audioTrack, sender);
        musicManager.scheduler.queue(audioTrack);

        if (musicManager.scheduler.playlistRepeating)
            musicManager.scheduler.setSavedQueue(guild, musicManager.scheduler.queue);

        if (new DedicatedChannelConfig().isChannelSet(musicManager.scheduler.getGuild().getId()))
            new DedicatedChannelConfig().updateMessage(musicManager.scheduler.getGuild());
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        List<AudioTrack> tracks = audioPlaylist.getTracks();

        if (trackUrl.startsWith("ytsearch:")) {
            if (announceMsg) {
                EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + tracks.get(0).getInfo().title
                        + "` by `" + tracks.get(0).getInfo().author + "`");
                if (event == null)
                    channel.sendMessageEmbeds(eb.build()).queue();
                else
                    event.replyEmbeds(eb.build()).setEphemeral(false).queue();
            } else {
                TogglesConfig toggleConfig = new TogglesConfig();
                DedicatedChannelConfig config = new DedicatedChannelConfig();

                if (config.isChannelSet(guild.getId()))
                    toggleConfig.setToggle(
                            guild, Toggles.ANNOUNCE_MESSAGES,
                            config.getOriginalAnnouncementToggle(guild.getId())
                    );
            }

            trackRequestedByUser.put(tracks.get(0), sender);
            musicManager.scheduler.queue(tracks.get(0));

            if (musicManager.scheduler.playlistRepeating)
                musicManager.scheduler.setSavedQueue(guild, musicManager.scheduler.queue);

            if (new DedicatedChannelConfig().isChannelSet(musicManager.scheduler.getGuild().getId()))
                new DedicatedChannelConfig().updateMessage(musicManager.scheduler.getGuild());
            return;
        }

        EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + tracks.size()
                + "` tracks from playlist `" + audioPlaylist.getName() + "`");
        if (event == null)
            channel.sendMessageEmbeds(eb.build()).queue();
        else
            event.replyEmbeds(eb.build()).setEphemeral(false).queue();

        for (final AudioTrack track : tracks) {
            trackRequestedByUser.put(track, sender);
            musicManager.scheduler.queue(track);
        }

        if (musicManager.scheduler.playlistRepeating)
            musicManager.scheduler.setSavedQueue(guild, musicManager.scheduler.queue);

        if (new DedicatedChannelConfig().isChannelSet(musicManager.scheduler.getGuild().getId()))
            new DedicatedChannelConfig().updateMessage(musicManager.scheduler.getGuild());
    }

    @Override
    public void noMatches() {
        EmbedBuilder eb = EmbedUtils.embedMessage("Nothing was found for `" + trackUrl.replace("ytsearch:", "") + "`. Try being more specific. *(Adding name of the artiste)*");
        if (event == null)
            msg.replyEmbeds(eb.build()).queue();
        else
            event.replyEmbeds(eb.build()).setEphemeral(false).queue();
    }

    @Override
    public void loadFailed(FriendlyException e) {
        if (musicManager.audioPlayer.getPlayingTrack() == null)
            guild.getAudioManager().closeAudioConnection();
        e.printStackTrace();

        EmbedBuilder eb = EmbedUtils.embedMessage("Error loading track");
        if (event == null)
            msg.replyEmbeds(eb.build()).queue();
        else
            event.replyEmbeds(eb.build()).setEphemeral(false).queue();
    }
}
