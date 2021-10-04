package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.model_objects.IPlaylistItem;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistRequest;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;
import lombok.Getter;
import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.main.Listener;
import main.main.Robertify;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private  final Map<Long, GuildMusicManager> musicManagers;
    @Getter
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return  this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildID) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager, guild);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void removeMusicManager(Guild guild) {
        musicManagers.remove(guild.getIdLong());
    }

    @SneakyThrows
    public void loadAndPlay(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        joinVoiceChannel(selfVoiceState, memberVoiceState, ctx);

        final GuildMusicManager musicManager = getMusicManager(channel.getGuild());

        if (trackUrl.contains("spotify.com")) {
            if (trackUrl.contains("/track/")) {
                String[] parsed = trackUrl.split("/track/");
                if (parsed.length == 2) {
                    final GetTrackRequest getTrackRequest = Robertify.getSpotifyApi().getTrack(parsed[1].replaceAll("\\?[a-zA-Z0-9~!@#$%^&*()\\-_=;:'\"|\\\\,./]*", "")).build();
                    Track track = getTrackRequest.execute();
                    trackUrl = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName();
                }
            } else if (trackUrl.contains("/playlist/")) {
                String[] parsed = trackUrl.split("/playlist/");
                if (parsed.length == 2) {
                    GetPlaylistRequest getPlaylistRequest = Robertify.getSpotifyApi().getPlaylist(parsed[1].replaceAll("\\?[a-zA-Z0-9~!@#$%^&*()\\-_=;:'\"|\\\\,./]*", "")).build();
                    Playlist playlist = getPlaylistRequest.execute();
                    PlaylistTrack[] tracks =  playlist.getTracks().getItems();
                    List<Track> trueTracks = new ArrayList<>();
                    Arrays.stream(tracks).forEach(track -> trueTracks.add((Track) track.getTrack()));
                    String[] finalTrackUrl = {trackUrl};

                    EmbedBuilder eb = EmbedUtils.embedMessage("Adding `" + trueTracks.size() + "` tracks from `" + playlist.getName() + "` to the queue...");
                    channel.sendMessageEmbeds(eb.build()).queue(msg -> {
                        long timeStarted = System.currentTimeMillis();
                        for (Track track : trueTracks) {
                            finalTrackUrl[0] = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName();
                            String finalTrackUrl1 = finalTrackUrl[0];
                            audioPlayerManager.loadItemOrdered(
                                    musicManager,
                                    finalTrackUrl[0],

                                    new AudioLoadResultHandler() {
                                        @Override
                                        public void trackLoaded(AudioTrack audioTrack) {
                                            musicManager.scheduler.queue(audioTrack);
                                        }

                                        @Override
                                        public void playlistLoaded(AudioPlaylist audioPlaylist) {
                                            List<AudioTrack> tracks = audioPlaylist.getTracks();
                                            musicManager.scheduler.queue(tracks.get(0));
                                        }

                                        @Override
                                        public void noMatches() {
                                            EmbedBuilder eb = EmbedUtils.embedMessage("Nothing was found for `"+ finalTrackUrl1 +"`. Try being more specific. *(Adding name of the artiste)*");
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
                        Listener.LOGGER.info("Took " + (System.currentTimeMillis()-timeStarted) + "ms to add "+trueTracks.size()+" tracks to the queue.");
                        EmbedBuilder lambdaEmbed = EmbedUtils.embedMessage("Finished adding `"+trueTracks.size()+"` tracks from `"+ playlist.getName() +"` to the queue!");
                        msg.editMessageEmbeds(lambdaEmbed.build()).queue();
                    });

                    return;
                }
            }
        }

        String finalTrackUrl = trackUrl;
        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + audioTrack.getInfo().title
                        + "` by `" + audioTrack.getInfo().author + "`");
                channel.sendMessageEmbeds(eb.build()).queue();

                musicManager.scheduler.queue(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                List<AudioTrack> tracks = audioPlaylist.getTracks();

                if (finalTrackUrl.startsWith("ytsearch:")) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + tracks.get(0).getInfo().title
                            + "` by `" + tracks.get(0).getInfo().author + "`");
                    channel.sendMessageEmbeds(eb.build()).queue();

                    musicManager.scheduler.queue(tracks.get(0));

                    return;
                }

                EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + tracks.size()
                + "` tracks from playlist `" + audioPlaylist.getName() + "`");
                channel.sendMessageEmbeds(eb.build()).queue();

                for (final AudioTrack track : tracks)
                    musicManager.scheduler.queue(track);

            }

            @Override
            public void noMatches() {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing was found for `"+finalTrackUrl+"`. Try being more specific. *(Adding name of the artiste)*");
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

    private void joinVoiceChannel(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        if (!selfVoiceState.inVoiceChannel()) {
            AudioManager audioManager = ctx.getGuild().getAudioManager();
            audioManager.openAudioConnection(memberVoiceState.getChannel());
            audioManager.setSelfDeafened(true);
        }
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new PlayerManager();
        return INSTANCE;
    }
}
