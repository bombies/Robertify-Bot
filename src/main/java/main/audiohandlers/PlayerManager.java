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
import main.audiohandlers.spotify.SpotifyAudioSourceManager;
import main.audiohandlers.youtube.LazyYoutubeAudioTrackFactory;
import main.commands.CommandContext;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.main.Listener;
import main.main.Robertify;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
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

        audioPlayerManager.registerSourceManager(new SpotifyAudioSourceManager(new YoutubeAudioSourceManager()));

        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
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

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        joinVoiceChannel(selfVoiceState, memberVoiceState);
        loadTrack(trackUrl, musicManager, channel, ctx, new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES));
    }

    public void loadAndPlay(String trackUrl, TextChannel announcementChannel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, SlashCommandEvent event) {
        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        joinVoiceChannel(selfVoiceState, memberVoiceState);
        loadTrack(trackUrl, musicManager, announcementChannel, new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES), event);
    }

    @SneakyThrows
    public void handleSpotifyURI(String spotifyURI, GuildMusicManager musicManager,
                                 TextChannel channel, CommandContext ctx,
                                 GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        var uri = SpotifyURI.parse(spotifyURI);

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

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                          TextChannel channel, CommandContext ctx, boolean announceMsg) {

        AudioLoader loader = new AudioLoader(musicManager, channel, trackRequestedByUser, ctx, trackUrl, announceMsg);
        var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           TextChannel announcementChannel, boolean announceMsg, SlashCommandEvent event) {

        AudioLoader loader = new AudioLoader(musicManager, announcementChannel, trackRequestedByUser, event, trackUrl, announceMsg);
        var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
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
