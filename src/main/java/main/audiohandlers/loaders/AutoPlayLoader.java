package main.audiohandlers.loaders;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoPlayLoader implements AudioLoadResultHandler {
    private final GuildMusicManager musicManager;
    private final Guild guild;
    private final TextChannel channel;

    public AutoPlayLoader(GuildMusicManager musicManager, TextChannel channel) {
        this.musicManager = musicManager;
        this.guild = musicManager.getGuild();
        this.channel = channel;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        throw new UnsupportedOperationException("This operation is not supported in the auto-play loader");
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult())
            throw new UnsupportedOperationException("This operation is not supported in the auto-play loader");

        final var scheduler = musicManager.getScheduler();

        HashMap<Long, List<String>> tracksRequestedByUsers = RobertifyAudioManager.getTracksRequestedByUsers();
        tracksRequestedByUsers.putIfAbsent(guild.getIdLong(), new ArrayList<>());

        List<AudioTrack> tracks = playlist.getTracks();
        for (final AudioTrack track : tracks.subList(1, tracks.size())) {
            tracksRequestedByUsers.get(guild.getIdLong()).add(guild.getSelfMember().getId() + ":" + track.getIdentifier());
            scheduler.queue(track);
        }

        if (scheduler.playlistRepeating) {
            scheduler.playlistRepeating = false;
            scheduler.getPastQueue().clear();
            scheduler.clearSavedQueue(guild);
        }

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        if (channel != null)
            channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Now playing recommended tracks")
                    .setTitle("Autoplay")
                    .setFooter("You can toggle autoplay off by running the \"autoplay\" command")
                    .build()
            ).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES));
    }

    @Override
    public void noMatches() {
        throw new FriendlyException("There were no similar tracks found!", FriendlyException.Severity.COMMON, new NullPointerException());
    }

    @Override
    public void loadFailed(FriendlyException exception) {

    }
}
