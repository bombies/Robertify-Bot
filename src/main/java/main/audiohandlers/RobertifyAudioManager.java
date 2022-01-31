package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.lavalink.LavaLinkGuildMusicManager;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RobertifyAudioManager {
    private static final Logger logger = LoggerFactory.getLogger(RobertifyAudioManager.class);

    private static RobertifyAudioManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final Map<Long, LavaLinkGuildMusicManager> lavalinkMusicManagers;
    @Getter
    private static final HashMap<AudioTrack, User> tracksRequestedByUsers = new HashMap<>();
    @Getter
    private static final List<AudioTrack> unannouncedTracks = new ArrayList<>();
    @Getter
    private final AudioPlayerManager audioPlayerManager;

    private RobertifyAudioManager() {
        this.musicManagers = new HashMap<>();
        this.lavalinkMusicManagers = new HashMap<>();
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

    public LavaLinkGuildMusicManager getLavaLinkMusicManager(Guild guild) {
        return this.lavalinkMusicManagers.computeIfAbsent(guild.getIdLong(), (gid) -> new LavaLinkGuildMusicManager(guild));
    }

    public void removeMusicManager(Guild guild) {
        this.musicManagers.remove(guild.getIdLong());
    }

    public void removeLavalinkMusicManager(Guild guild) {
        this.lavalinkMusicManagers.get(guild.getIdLong()).destroy();
        this.lavalinkMusicManagers.remove(guild.getIdLong());
    }

    @SneakyThrows
    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, CommandContext ctx,
                            Message botMsg, boolean addToBeginning) {

        final var musicManager = getLavaLinkMusicManager(memberVoiceState.getGuild());
        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
//            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
            joinVCLavaLink(ctx.getChannel(), memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                ctx,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                addToBeginning
        );
    }

    @SneakyThrows
    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, TextChannel channel,
                            User user, Message botMsg, boolean addToBeginning) {

        final var musicManager = getLavaLinkMusicManager(memberVoiceState.getGuild());
        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
//            joinVoiceChannel(channel, selfVoiceState, memberVoiceState);
            joinVCLavaLink(channel, memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                user,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                addToBeginning
        );
    }

    @SneakyThrows
    public void loadAndPlayShuffled(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, CommandContext ctx,
                                    Message botMsg, boolean addToBeginning) {
        final var musicManager = getLavaLinkMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
//            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
            joinVCLavaLink(ctx.getChannel(), memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadPlaylistShuffled(
                trackUrl,
                musicManager,
                ctx,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                addToBeginning
        );
    }

    @SneakyThrows
    public void loadAndPlayFromDedicatedChannel(String trackUrl, GuildVoiceState selfVoiceState,
                                                GuildVoiceState memberVoiceState, CommandContext ctx, Message botMsg,
                                                boolean addToBeginning) {

        final var musicManager = getLavaLinkMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
//            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
            joinVCLavaLink(ctx.getChannel(), memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                ctx,
                false,
                botMsg,
                addToBeginning
        );
    }

    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg,
                            SlashCommandEvent event, boolean addToBeginning) {
        final var musicManager = getLavaLinkMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
//            joinVoiceChannel(event.getTextChannel(), selfVoiceState, memberVoiceState);
            joinVCLavaLink(event.getTextChannel(), memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                event.getUser(),
                addToBeginning
        );
    }

    public void loadAndPlayShuffled(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg, SlashCommandEvent event,
                                    boolean addToBeginning) {
        final var musicManager = getLavaLinkMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
//            joinVoiceChannel(event.getTextChannel(), selfVoiceState, memberVoiceState);
            joinVCLavaLink(event.getTextChannel(), memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadPlaylistShuffled(
                trackUrl,
                musicManager,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                event.getUser(),
                addToBeginning
        );
    }

    public void loadAndPlayFromDedicatedChannel(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg, SlashCommandEvent event,
                                                boolean addToBeginning) {
        final var musicManager = getLavaLinkMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
//            joinVoiceChannel(event.getTextChannel(), selfVoiceState, memberVoiceState);
            joinVCLavaLink(event.getTextChannel(), memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                false,
                botMsg,
                event.getUser(),
                addToBeginning
        );
    }

    public void loadAndPlayLocal(TextChannel channel, String path, GuildVoiceState selfVoiceState,
                                 GuildVoiceState memberVoiceState, CommandContext ctx, Message botMsg,
                                 boolean addToBeginning) {
        final var musicManager = getLavaLinkMusicManager(channel.getGuild());

        try {
//            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
            joinVCLavaLink(ctx.getChannel(), memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }
        loadTrack(
                path,
                musicManager,
                ctx,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                addToBeginning
        );
    }

    private void loadTrack(String trackUrl, AbstractMusicManager musicManager,
                           User user, boolean announceMsg, Message botMsg,
                           boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(user, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false, addToBeginning);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadTrack(String trackUrl, AbstractMusicManager musicManager,
                           CommandContext ctx, boolean announceMsg, Message botMsg,
                           boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(ctx.getAuthor(), musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false, addToBeginning);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadTrack(String trackUrl, AbstractMusicManager musicManager,
                           boolean announceMsg, Message botMsg, User sender,
                           boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false, addToBeginning);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadPlaylistShuffled(String trackUrl, AbstractMusicManager musicManager,
                                      CommandContext ctx, boolean announceMsg, Message botMsg,
                                      boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(ctx.getAuthor(), musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, true, addToBeginning);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadPlaylistShuffled(String trackUrl, AbstractMusicManager musicManager,
                                      boolean announceMsg, Message botMsg, User sender,
                                      boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, true, addToBeginning);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    public void joinVCLavaLink(TextChannel channel, VoiceChannel vc, LavaLinkGuildMusicManager musicManager) {
        try {
            musicManager.getLink().connect(vc);
        } catch (InsufficientPermissionException e) {
            if (channel != null)
                channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(channel.getGuild(), "I do not have enough permissions to join " + vc.getAsMention()).build())
                        .queue();
            throw e;
        }
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
