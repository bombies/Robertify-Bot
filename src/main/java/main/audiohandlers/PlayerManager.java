package main.audiohandlers;

import com.google.inject.*;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.youtube.LazyYoutubeAudioTrackFactory;
import main.commands.CommandContext;
import main.main.Listener;
import main.main.Robertify;
import main.utils.spotify.SpotifySourceManager;
import main.utils.spotify.SpotifyURI;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.util.*;

public class PlayerManager extends AbstractModule {
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    @Getter
    private static final HashMap<AudioTrack, User> trackRequestedByUser = new HashMap<>();
    @Getter
    private final AudioPlayerManager audioPlayerManager;

    @Override
    protected void configure() {
        bind(AudioTrackFactory.class).to(LazyYoutubeAudioTrackFactory.class);
    }

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    @Provides
    @Singleton
    SpotifySourceManager spotifySourceManager(AudioTrackFactory trackFactory) {
        return new SpotifySourceManager(trackFactory);
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
        final GuildMusicManager musicManager = getMusicManager(channel.getGuild());

        if (trackUrl.contains("spotify.com")) {
            handleSpotifyURI(trackUrl, musicManager, channel, ctx, selfVoiceState, memberVoiceState);
            return;
        } else if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        joinVoiceChannel(selfVoiceState, memberVoiceState);
        loadTrack(trackUrl, musicManager, channel, ctx, true);
    }

    public void loadAndPlay(String trackUrl, TextChannel announcementChannel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, SlashCommandEvent event) {
        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("spotify.com")) {
            handleSpotifyURI(trackUrl, musicManager, announcementChannel, selfVoiceState, memberVoiceState, event);
            return;
        } else if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        joinVoiceChannel(selfVoiceState, memberVoiceState);
        loadTrack(trackUrl, musicManager, announcementChannel, true, event);
    }

    @SneakyThrows
    public void handleSpotifyURI(String spotifyURI, GuildMusicManager musicManager,
                                 TextChannel channel, CommandContext ctx,
                                 GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        var uri = SpotifyURI.parse(spotifyURI);

        LazyYoutubeAudioTrackFactory lazyYoutubeAudioTrackFactory = new LazyYoutubeAudioTrackFactory(new YoutubeSearchProvider(), audioPlayerManager.source(YoutubeAudioSourceManager.class));
        audioPlayerManager.registerSourceManager(spotifySourceManager(lazyYoutubeAudioTrackFactory));

        switch (uri.getType()) {
            case TRACK -> {
                joinVoiceChannel(selfVoiceState, memberVoiceState);
                final GetTrackRequest getTrackRequest = Robertify.getSpotifyApi().getTrack(uri.getId()).build();
                Track track = getTrackRequest.execute();
                spotifyURI = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio";
                loadTrack(spotifyURI, musicManager, channel, ctx, true);
            }
            case ALBUM -> {
                joinVoiceChannel(selfVoiceState, memberVoiceState);
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
                EmbedBuilder eb = EmbedUtils.embedMessage("Sorry, spotify playlists aren't available right now.");
                ctx.getMessage().replyEmbeds(eb.build()).queue();

//                GetPlaylistRequest getPlaylistRequest = Robertify.getSpotifyApi().getPlaylist(uri.getId()).build();
//                Playlist playlist = getPlaylistRequest.execute();
//                PlaylistTrack[] tracks = playlist.getTracks().getItems();
//                List<Track> trueTracks = new ArrayList<>();
//
//                EmbedBuilder eb = EmbedUtils.embedMessage("Adding `" + tracks.length + "` tracks from `" + playlist.getName() + "` to the queue...");
//                ctx.getMessage().replyEmbeds(eb.build()).queue(msg ->{
//                    Arrays.stream(tracks).forEach(track -> trueTracks.add((Track) track.getTrack()));
//                    trueTracks.forEach(track -> loadTrack(
//                            "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio",
//                            musicManager, channel, ctx, false
//                    ));
//                });
            }
        }
    }

    @SneakyThrows
    public void handleSpotifyURI(String spotifyURI, GuildMusicManager musicManager, TextChannel announcementChannel,
                                 GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, SlashCommandEvent event) {
        var uri = SpotifyURI.parse(spotifyURI);

        LazyYoutubeAudioTrackFactory lazyYoutubeAudioTrackFactory = new LazyYoutubeAudioTrackFactory(new YoutubeSearchProvider(), audioPlayerManager.source(YoutubeAudioSourceManager.class));
        audioPlayerManager.registerSourceManager(spotifySourceManager(lazyYoutubeAudioTrackFactory));

        switch (uri.getType()) {
            case TRACK -> {
                joinVoiceChannel(selfVoiceState, memberVoiceState);
                final GetTrackRequest getTrackRequest = Robertify.getSpotifyApi().getTrack(uri.getId()).build();
                Track track = getTrackRequest.execute();
                spotifyURI = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio";
                loadTrack(spotifyURI, musicManager, announcementChannel, true, event);
            }
            case ALBUM -> {
                joinVoiceChannel(selfVoiceState, memberVoiceState);
                GetAlbumRequest getAlbumRequest = Robertify.getSpotifyApi().getAlbum(uri.getId()).build();
                Album album = getAlbumRequest.execute();
                TrackSimplified[] tracks = album.getTracks().getItems();

                EmbedBuilder eb = EmbedUtils.embedMessage("Adding `" + tracks.length + "` tracks from `" + album.getName() + "` to the queue...");
                event.replyEmbeds(eb.build()).queue(msg ->{
                    Arrays.stream(tracks).forEach(track -> loadTrack(
                            "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio",
                            musicManager, announcementChannel, false, event
                    ));
                });
            }
            case PLAYLIST -> {
                EmbedBuilder eb = EmbedUtils.embedMessage("Sorry, spotify playlists aren't available right now.");
                event.replyEmbeds(eb.build()).queue();

//                GetPlaylistRequest getPlaylistRequest = Robertify.getSpotifyApi().getPlaylist(uri.getId()).build();
//                Playlist playlist = getPlaylistRequest.execute();
//                PlaylistTrack[] tracks = playlist.getTracks().getItems();
//                List<Track> trueTracks = new ArrayList<>();
//
//                EmbedBuilder eb = EmbedUtils.embedMessage("Adding `" + tracks.length + "` tracks from `" + playlist.getName() + "` to the queue...");
//                ctx.getMessage().replyEmbeds(eb.build()).queue(msg ->{
//                    Arrays.stream(tracks).forEach(track -> trueTracks.add((Track) track.getTrack()));
//                    trueTracks.forEach(track -> loadTrack(
//                            "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName() + " audio",
//                            musicManager, channel, ctx, false
//                    ));
//                });
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

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           TextChannel announcementChannel, boolean announceMsg, SlashCommandEvent event) {

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                if (announceMsg) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + audioTrack.getInfo().title
                            + "` by `" + audioTrack.getInfo().author + "`");
                    event.replyEmbeds(eb.build()).queue();
                }

                trackRequestedByUser.put(audioTrack, event.getUser());
                musicManager.scheduler.queue(audioTrack);

                if (musicManager.scheduler.playlistRepeating)
                    musicManager.scheduler.setSavedQueue(event.getGuild(), musicManager.scheduler.queue);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                List<AudioTrack> tracks = audioPlaylist.getTracks();

                if (trackUrl.startsWith("ytsearch:")) {
                    if (announceMsg) {
                        EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + tracks.get(0).getInfo().title
                                + "` by `" + tracks.get(0).getInfo().author + "`");
                        event.replyEmbeds(eb.build()).setEphemeral(false).queue();
                    }

                    trackRequestedByUser.put(tracks.get(0), event.getUser());
                    musicManager.scheduler.queue(tracks.get(0));

                    if (musicManager.scheduler.playlistRepeating)
                        musicManager.scheduler.setSavedQueue(event.getGuild(), musicManager.scheduler.queue);
                    return;
                }

                EmbedBuilder eb = EmbedUtils.embedMessage("Adding to queue: `" + tracks.size()
                        + "` tracks from playlist `" + audioPlaylist.getName() + "`");
                event.replyEmbeds(eb.build()).setEphemeral(false).queue();

                for (final AudioTrack track : tracks) {
                    trackRequestedByUser.put(track, event.getUser());
                    musicManager.scheduler.queue(track);
                }

                if (musicManager.scheduler.playlistRepeating)
                    musicManager.scheduler.setSavedQueue(event.getGuild(), musicManager.scheduler.queue);
            }

            @Override
            public void noMatches() {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing was found for `" + trackUrl.replace("ytsearch:", "") + "`. Try being more specific. *(Adding name of the artiste)*");
                event.replyEmbeds(eb.build()).setEphemeral(false).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                if (musicManager.audioPlayer.getPlayingTrack() == null)
                    event.getGuild().getAudioManager().closeAudioConnection();
                e.printStackTrace();

                EmbedBuilder eb = EmbedUtils.embedMessage("Error loading track");
                event.replyEmbeds(eb.build()).setEphemeral(false).queue();
            }
        });
    }

    public void joinVoiceChannel(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        if (!selfVoiceState.inVoiceChannel()) {
            AudioManager audioManager = selfVoiceState.getGuild().getAudioManager();
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
