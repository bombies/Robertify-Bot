package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.data.albums.GetAlbumRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistRequest;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;
import lombok.Getter;
import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.main.Listener;
import main.main.Robertify;
import main.utils.spotify.SpotifyURI;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.managers.AudioManager;

import java.net.URI;
import java.util.*;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private static final HashMap<AudioTrack, User> trackRequestedByUser = new HashMap<>();
    @Getter
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildID) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager, guild);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    @SneakyThrows
    public void loadAndPlay(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        joinVoiceChannel(selfVoiceState, memberVoiceState, ctx);

        final GuildMusicManager musicManager = getMusicManager(channel.getGuild());

        if (trackUrl.contains("spotify.com")) {
            handleSpotifyURI(trackUrl, musicManager, channel, ctx);
            return;
        } else if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        loadTrack(trackUrl, musicManager, channel, ctx, true);
    }

    @SneakyThrows
    public void handleSpotifyURI(String spotifyURI, GuildMusicManager musicManager,
                                 TextChannel channel, CommandContext ctx) {
        var uri = SpotifyURI.parse(spotifyURI);

        switch (uri.getType()) {
            case TRACK -> {
                final GetTrackRequest getTrackRequest = Robertify.getSpotifyApi().getTrack(uri.getId()).build();
                Track track = getTrackRequest.execute();
                spotifyURI = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio";
                loadTrack(spotifyURI, musicManager, channel, ctx, true);
            }
            case ALBUM -> {
                GetAlbumRequest getAlbumRequest = Robertify.getSpotifyApi().getAlbum(uri.getId()).build();
                Album album = getAlbumRequest.execute();
                TrackSimplified[] tracks = album.getTracks().getItems();

                EmbedBuilder eb = EmbedUtils.embedMessage("Adding `" + tracks.length + "` tracks from `" + album.getName() + "` to the queue...");
                ctx.getMessage().replyEmbeds(eb.build()).queue(msg ->{
                    Arrays.stream(tracks).forEach(track -> loadTrack(
                            "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio",
                            musicManager, channel, ctx, false
                    ));
                });
            }
            case PLAYLIST -> {
                GetPlaylistRequest getPlaylistRequest = Robertify.getSpotifyApi().getPlaylist(uri.getId()).build();
                Playlist playlist = getPlaylistRequest.execute();
                PlaylistTrack[] tracks = playlist.getTracks().getItems();
                List<Track> trueTracks = new ArrayList<>();

                EmbedBuilder eb = EmbedUtils.embedMessage("Adding `" + tracks.length + "` tracks from `" + playlist.getName() + "` to the queue...");
                ctx.getMessage().replyEmbeds(eb.build()).queue(msg ->{
                    Arrays.stream(tracks).forEach(track -> trueTracks.add((Track) track.getTrack()));
                    trueTracks.forEach(track -> loadTrack(
                            "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio",
                            musicManager, channel, ctx, false
                    ));
                });
            }
        }
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                          TextChannel channel, CommandContext ctx, boolean announceMsg) {

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                if (announceMsg) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + audioTrack.getInfo().title
                            + "` by `" + audioTrack.getInfo().author + "`");
                    channel.sendMessageEmbeds(eb.build()).queue();
                }

                trackRequestedByUser.put(audioTrack, ctx.getAuthor());
                musicManager.scheduler.queue(audioTrack);

                if (musicManager.scheduler.playlistRepeating)
                    musicManager.scheduler.setSavedQueue(ctx.getGuild(), musicManager.scheduler.queue);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                List<AudioTrack> tracks = audioPlaylist.getTracks();

                if (trackUrl.startsWith("ytsearch:")) {
                    if (announceMsg) {
                        EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + tracks.get(0).getInfo().title
                                + "` by `" + tracks.get(0).getInfo().author + "`");
                        channel.sendMessageEmbeds(eb.build()).queue();
                    }

                    trackRequestedByUser.put(tracks.get(0), ctx.getAuthor());
                    musicManager.scheduler.queue(tracks.get(0));

                    if (musicManager.scheduler.playlistRepeating)
                        musicManager.scheduler.setSavedQueue(ctx.getGuild(), musicManager.scheduler.queue);
                    return;
                }

                EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + tracks.size()
                        + "` tracks from playlist `" + audioPlaylist.getName() + "`");
                channel.sendMessageEmbeds(eb.build()).queue();

                for (final AudioTrack track : tracks) {
                    trackRequestedByUser.put(track, ctx.getAuthor());
                    musicManager.scheduler.queue(track);
                }

                if (musicManager.scheduler.playlistRepeating)
                    musicManager.scheduler.setSavedQueue(ctx.getGuild(), musicManager.scheduler.queue);
            }

            @Override
            public void noMatches() {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing was found for `" + trackUrl.replace("ytsearch:", "") + "`. Try being more specific. *(Adding name of the artiste)*");
                ctx.getMessage().replyEmbeds(eb.build()).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                if (musicManager.audioPlayer.getPlayingTrack() == null)
                    ctx.getGuild().getAudioManager().closeAudioConnection();
                e.printStackTrace();

                EmbedBuilder eb = EmbedUtils.embedMessage("Error loading track");
                ctx.getMessage().replyEmbeds(eb.build()).queue();
            }
        });
    }

    public void joinVoiceChannel(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        if (!selfVoiceState.inVoiceChannel()) {
            AudioManager audioManager = ctx.getGuild().getAudioManager();
            audioManager.openAudioConnection(memberVoiceState.getChannel());
            audioManager.setSelfDeafened(true);
        }
    }

    public static User getRequester(AudioTrack track) {
        return trackRequestedByUser.get(track);
    }

    public static void removeRequester(AudioTrack track, User requester) {
        trackRequestedByUser.remove(track, requester);
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new PlayerManager();
        return INSTANCE;
    }
}
