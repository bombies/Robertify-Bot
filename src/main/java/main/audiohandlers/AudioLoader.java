package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.commands.CommandContext;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.HashMap;
import java.util.List;

public class AudioLoader implements AudioLoadResultHandler {
    private final GuildMusicManager musicManager;
    private final boolean announceMsg;
    private final TextChannel channel;
    private final HashMap<AudioTrack, User> trackRequestedByUser;
    private final CommandContext ctx;
    private final String trackUrl;
    private final SlashCommandEvent event;


    public AudioLoader(GuildMusicManager musicManager, TextChannel channel, HashMap<AudioTrack, User> trackRequestedByUser,
                       CommandContext ctx, String trackUrl, boolean announceMsg) {
        this.musicManager = musicManager;
        this.channel = channel;
        this.trackRequestedByUser = trackRequestedByUser;
        this.ctx = ctx;
        this.trackUrl = trackUrl;
        this.announceMsg = announceMsg;
        this.event = null;
    }

    public AudioLoader(GuildMusicManager musicManager, TextChannel channel, HashMap<AudioTrack, User> trackRequestedByUser,
                       SlashCommandEvent event, String trackUrl, boolean announceMsg) {
        this.musicManager = musicManager;
        this.channel = channel;
        this.trackRequestedByUser = trackRequestedByUser;
        this.ctx = null;
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

            if (config.isChannelSet(ctx.getGuild().getId()))
                toggleConfig.setToggle(
                        ctx.getGuild(), Toggles.ANNOUNCE_MESSAGES,
                        config.getOriginalAnnouncementToggle(ctx.getGuild().getId())
                );
        }

        trackRequestedByUser.put(audioTrack, ctx.getAuthor());
        musicManager.scheduler.queue(audioTrack);

        if (musicManager.scheduler.playlistRepeating)
            musicManager.scheduler.setSavedQueue(ctx.getGuild(), musicManager.scheduler.queue);

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

                if (config.isChannelSet(ctx.getGuild().getId()))
                    toggleConfig.setToggle(
                            ctx.getGuild(), Toggles.ANNOUNCE_MESSAGES,
                            config.getOriginalAnnouncementToggle(ctx.getGuild().getId())
                    );
            }

            trackRequestedByUser.put(tracks.get(0), ctx.getAuthor());
            musicManager.scheduler.queue(tracks.get(0));

            if (musicManager.scheduler.playlistRepeating)
                musicManager.scheduler.setSavedQueue(ctx.getGuild(), musicManager.scheduler.queue);

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
            trackRequestedByUser.put(track, ctx.getAuthor());
            musicManager.scheduler.queue(track);
        }

        if (musicManager.scheduler.playlistRepeating)
            musicManager.scheduler.setSavedQueue(ctx.getGuild(), musicManager.scheduler.queue);

        if (new DedicatedChannelConfig().isChannelSet(musicManager.scheduler.getGuild().getId()))
            new DedicatedChannelConfig().updateMessage(musicManager.scheduler.getGuild());
    }

    @Override
    public void noMatches() {
        EmbedBuilder eb = EmbedUtils.embedMessage("Nothing was found for `" + trackUrl.replace("ytsearch:", "") + "`. Try being more specific. *(Adding name of the artiste)*");
        if (event == null)
            ctx.getMessage().replyEmbeds(eb.build()).queue();
        else
            event.replyEmbeds(eb.build()).setEphemeral(false).queue();
    }

    @Override
    public void loadFailed(FriendlyException e) {
        if (musicManager.audioPlayer.getPlayingTrack() == null)
            ctx.getGuild().getAudioManager().closeAudioConnection();
        e.printStackTrace();

        EmbedBuilder eb = EmbedUtils.embedMessage("Error loading track");
        if (event == null)
            ctx.getMessage().replyEmbeds(eb.build()).queue();
        else
            event.replyEmbeds(eb.build()).setEphemeral(false).queue();
    }
}
