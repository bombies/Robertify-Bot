package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.*;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.lavaplayer.GuildMusicManager;
import main.audiohandlers.sources.deezer.DeezerAudioSourceManager;
import main.audiohandlers.sources.spotify.SpotifyAudioSourceManager;
import main.commands.CommandContext;
import main.constants.Toggles;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RobertifyAudioManager {
    private static final Logger logger = LoggerFactory.getLogger(RobertifyAudioManager.class);

    private static RobertifyAudioManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    @Getter
    private static final HashMap<AudioTrack, User> tracksRequestedByUsers = new HashMap<>();
    @Getter
    private static final List<AudioTrack> unannouncedTracks = new ArrayList<>();
    @Getter
    private final AudioPlayerManager audioPlayerManager;

    private RobertifyAudioManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        audioPlayerManager.registerSourceManager(new SpotifyAudioSourceManager(new YoutubeAudioSourceManager(), SoundCloudAudioSourceManager.createDefault()));
        audioPlayerManager.registerSourceManager(new DeezerAudioSourceManager(new YoutubeAudioSourceManager(), SoundCloudAudioSourceManager.createDefault()));
        audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
        audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        audioPlayerManager.registerSourceManager(new BandcampAudioSourceManager());
        audioPlayerManager.registerSourceManager(new VimeoAudioSourceManager());
        audioPlayerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new BeamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new HttpAudioSourceManager());
        audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildID) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager, guild);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void removeMusicManager(Guild guild) {
        this.musicManagers.remove(guild.getIdLong());
    }

    @SneakyThrows
    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, CommandContext ctx, Message botMsg) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());
        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                ctx,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg
        );
    }

    @SneakyThrows
    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, TextChannel channel, User user, Message botMsg) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());
        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(channel, selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                user,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg
        );
    }

    @SneakyThrows
    public void loadAndPlayShuffled(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, CommandContext ctx, Message botMsg) {
        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }

        loadPlaylistShuffled(
                trackUrl,
                musicManager,
                ctx,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg
        );
    }

    @SneakyThrows
    public void loadAndPlayFromDedicatedChannel(String trackUrl, GuildVoiceState selfVoiceState,
                                                GuildVoiceState memberVoiceState, CommandContext ctx, Message botMsg) {

        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                ctx,
                false,
                botMsg
        );
    }

    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg, SlashCommandEvent event) {
        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(event.getTextChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                event.getUser()
        );
    }

    public void loadAndPlayShuffled(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg, SlashCommandEvent event) {
        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(event.getTextChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }

        loadPlaylistShuffled(
                trackUrl,
                musicManager,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                event.getUser()
        );
    }

    public void loadAndPlayFromDedicatedChannel(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg, SlashCommandEvent event) {
        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(event.getTextChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                false,
                botMsg,
                event.getUser()
        );
    }

    public void loadAndPlayLocal(TextChannel channel, String path, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx, Message botMsg) {
        final GuildMusicManager musicManager = getMusicManager(channel.getGuild());

        try {
            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }
        loadTrack(
                path,
                musicManager,
                ctx,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg
        );
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           User user, boolean announceMsg, Message botMsg) {

        final AudioLoader loader = new AudioLoader(user, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           CommandContext ctx, boolean announceMsg, Message botMsg) {

        final AudioLoader loader = new AudioLoader(ctx.getAuthor(), musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           boolean announceMsg, Message botMsg, User sender) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadPlaylistShuffled(String trackUrl, GuildMusicManager musicManager,
                           CommandContext ctx, boolean announceMsg, Message botMsg) {

        final AudioLoader loader = new AudioLoader(ctx.getAuthor(), musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, true);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadPlaylistShuffled(String trackUrl, GuildMusicManager musicManager,
                           boolean announceMsg, Message botMsg, User sender) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, true);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    public void joinVoiceChannel(TextChannel channel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        if (!selfVoiceState.inVoiceChannel()) {
            final AudioManager audioManager = selfVoiceState.getGuild().getAudioManager();
            final var vc = memberVoiceState.getChannel();
            final var guild = channel.getGuild();

            try {
                audioManager.openAudioConnection(memberVoiceState.getChannel());

                if (vc.getType().equals(ChannelType.STAGE)) {
                    final var self = selfVoiceState.getMember();

                    if (self.hasPermission(vc, Permission.REQUEST_TO_SPEAK) ||
                            self.hasPermission(vc, Permission.VOICE_MUTE_OTHERS)) {
                        vc.getGuild().requestToSpeak();
                    } else {
                        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I need to have the `"+Permission.REQUEST_TO_SPEAK.getName()+"` permission " +
                                        "for me to properly play music in this stage channel.")
                                .build()).queue();
                    }
                } else audioManager.setSelfDeafened(true);
            } catch (InsufficientPermissionException e) {
                channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I do not have enough permissions to join " + memberVoiceState.getChannel().getAsMention()).build())
                        .queue();
                throw e;
            }
        }
    }

    public static User getRequester(AudioTrack track) {
        return tracksRequestedByUsers.get(track);
    }

    public static void removeRequester(AudioTrack track, User requester) {
        tracksRequestedByUsers.remove(track, requester);
    }

    public static RobertifyAudioManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new RobertifyAudioManager();
        return INSTANCE;
    }
}
