package main.audiohandlers.loaders;

import lavalink.client.io.FriendlyException;
import lavalink.client.io.LoadResultHandler;
import lavalink.client.player.track.AudioPlaylist;
import lavalink.client.player.track.AudioTrack;
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

public class AutoPlayLoader implements LoadResultHandler {
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
        final var scheduler = musicManager.getScheduler();

        HashMap<Long, List<String>> tracksRequestedByUsers = RobertifyAudioManager.getTracksRequestedByUsers();
        tracksRequestedByUsers.putIfAbsent(guild.getIdLong(), new ArrayList<>());

        List<AudioTrack> tracks = playlist.getTracks();
        for (final AudioTrack track : tracks.subList(1, tracks.size())) {
            tracksRequestedByUsers.get(guild.getIdLong()).add(guild.getSelfMember().getId() + ":" + track.getTrack());
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
    public void searchResultLoaded(List<AudioTrack> tracks) {
        throw new UnsupportedOperationException("This operation is not supported in the auto-play loader");
    }

    @Override
    public void noMatches() {
        throw new FriendlyException("There were no similar tracks found!", FriendlyException.Severity.COMMON, new NullPointerException());
    }

    @Override
    public void loadFailed(FriendlyException exception) {

    }
}
