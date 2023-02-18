package main.audiohandlers;

import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager;
import com.github.topisenpai.lavasrc.deezer.DeezerAudioSourceManager;
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.loaders.AudioLoader;
import main.audiohandlers.loaders.AutoPlayLoader;
import main.audiohandlers.loaders.SearchResultLoader;
import main.commands.prefixcommands.CommandContext;
import main.constants.ENV;
import main.constants.Toggles;
import main.main.Config;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RobertifyAudioManager {
    private static final Logger logger = LoggerFactory.getLogger(RobertifyAudioManager.class);

    private static RobertifyAudioManager INSTANCE;

    @Getter
    private final Map<Long, GuildMusicManager> musicManagers;

    /**
     * Each guild will have a list that consists of tracks formatted "userid:trackstring"
     */
    @Getter
    private static final HashMap<Long, List<String>> tracksRequestedByUsers = new HashMap<>();

    @Getter
    private static final List<String> unannouncedTracks = new ArrayList<>();

    @Getter
    private final AudioPlayerManager playerManager;

    private RobertifyAudioManager() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        // TODO IPv6 rotation stuff
        // Snippet acquired from Mantaro Bot
//        if (true) {
//            AbstractRoutePlanner planner;
//            String block = "";
//            List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(block));
//
//            if (true) {
//                planner = new RotatingNanoIpRoutePlanner(blocks);
//            } else {
//                try {
//                    var blacklistedGW = InetAddress.getByName("host");
//                    planner = new RotatingNanoIpRoutePlanner(blocks, inetAddress ->
//                       !inetAddress.equals(blacklistedGW)
//                    );
//                } catch (Exception e) {
//                    planner = new RotatingNanoIpRoutePlanner(blocks);
//                    logger.error("An unexpected error occurred!", e);
//                }
//            }
//
//            new YoutubeIpRotatorSetup(planner)
//                    .forSource(youtubeAudioSourceManager)
//                    .setup();
//        }

        AudioSourceManagers.registerLocalSource(this.playerManager);
        this.playerManager.registerSourceManager(new SpotifySourceManager(Config.getProviders(), Config.get(ENV.SPOTIFY_CLIENT_ID), Config.get(ENV.SPOTIFY_CLIENT_SECRET), "us", this.playerManager));
        this.playerManager.registerSourceManager(new AppleMusicSourceManager(Config.getProviders(), null, "us", this.playerManager));
        this.playerManager.registerSourceManager(new DeezerAudioSourceManager(Config.get(ENV.DEEZER_ACCESS_TOKEN)));
        AudioSourceManagers.registerRemoteSources(this.playerManager);

    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (gid) -> new GuildMusicManager(guild));
    }

    public void removeMusicManager(Guild guild) {
        this.musicManagers.get(guild.getIdLong()).destroy();
        this.musicManagers.remove(guild.getIdLong());
    }

    @SneakyThrows
    public void loadAndPlay(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState,
                            Message botMsg, boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(channel, voiceChannel, musicManager);
                    loadTrack(
                            trackUrl,
                            musicManager,
                            memberVoiceState.getMember().getUser(),
                            new TogglesConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    @SneakyThrows
    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, TextChannel channel,
                            User user, Message botMsg, boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(channel, voiceChannel, musicManager);
                    loadTrack(
                            trackUrl,
                            musicManager,
                            user,
                            new TogglesConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    @SneakyThrows
    public void loadAndPlayShuffled(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, CommandContext ctx,
                                    Message botMsg, boolean addToBeginning) {
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(ctx.getChannel(), voiceChannel, musicManager);
                    loadPlaylistShuffled(
                            memberVoiceState.getMember().getUser(),
                            trackUrl,
                            musicManager,
                            new TogglesConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    @SneakyThrows
    public void loadAndPlayFromDedicatedChannel(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
                                                GuildVoiceState memberVoiceState, Message botMsg,
                                                boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(channel, voiceChannel, musicManager);
                    loadTrack(
                            trackUrl,
                            musicManager,
                            memberVoiceState.getMember().getUser(),
                            false,
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    @SneakyThrows
    public void loadAndPlayFromDedicatedChannelShuffled(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
                                                GuildVoiceState memberVoiceState, Message botMsg,
                                                boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(channel, voiceChannel, musicManager);
                    loadPlaylistShuffled(
                            memberVoiceState.getMember().getUser(),
                            trackUrl,
                            musicManager,
                            false,
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg,
                            SlashCommandInteractionEvent event, boolean addToBeginning) {
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(event.getChannel().asTextChannel(), voiceChannel, musicManager);
                    loadTrack(
                            trackUrl,
                            musicManager,
                            new TogglesConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            event.getUser(),
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
            return;
        }
    }

    public void loadAndPlayShuffled(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg, SlashCommandInteractionEvent event,
                                    boolean addToBeginning) {
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(event.getChannel().asTextChannel(), voiceChannel, musicManager);
                    loadPlaylistShuffled(
                            trackUrl,
                            musicManager,
                            new TogglesConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            event.getUser(),
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    public void loadAndPlayFromDedicatedChannel(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg, SlashCommandInteractionEvent event,
                                                boolean addToBeginning) {
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(event.getChannel().asTextChannel(), voiceChannel, musicManager);
                    loadTrack(
                            trackUrl,
                            musicManager,
                            false,
                            botMsg,
                            event.getUser(),
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    public void loadAndPlayLocal(TextChannel channel, String path, GuildVoiceState selfVoiceState,
                                 GuildVoiceState memberVoiceState, Message botMsg,
                                 boolean addToBeginning) {
        final var musicManager = getMusicManager(channel.getGuild());

        try {
            final var voiceChannel = memberVoiceState.getChannel();
            if (voiceChannel != null)
                if (voiceChannel.getMembers().size() != 0) {
                    joinAudioChannel(channel, voiceChannel, musicManager);
                    loadTrack(
                            path,
                            musicManager,
                            memberVoiceState.getMember().getUser(),
                            new TogglesConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           User user, boolean announceMsg, Message botMsg,
                           boolean addToBeginning) {
        final AudioLoader loader = new AudioLoader(user, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false, addToBeginning);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackUrl, loader);
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           boolean announceMsg, Message botMsg, User sender,
                           boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, false, addToBeginning);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackUrl, loader);
    }

    public void loadSearchResults(GuildMusicManager musicManager, User searcher, Message botMsg, String query) {
        final SearchResultLoader loader = new SearchResultLoader(musicManager.getGuild(), searcher, query, botMsg);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, query, loader);
    }

    public void loadSearchResults(GuildMusicManager musicManager, User searcher, InteractionHook botMsg, String query) {
        final SearchResultLoader loader = new SearchResultLoader(musicManager.getGuild(), searcher, query, botMsg);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, query, loader);
    }

    public void loadRecommendedTracks(GuildMusicManager musicManager, TextChannel channel, AudioTrack query) {
        final AutoPlayLoader loader = new AutoPlayLoader(musicManager, channel);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, SpotifySourceManager.RECOMMENDATIONS_PREFIX + "seed_tracks=" + query.getIdentifier(), loader);
    }

    private void loadPlaylistShuffled(User requester, String trackUrl, GuildMusicManager musicManager, boolean announceMsg, Message botMsg,
                                      boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(requester, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, true, addToBeginning);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackUrl, loader);
    }

    private void loadPlaylistShuffled(String trackUrl, GuildMusicManager musicManager,
                                      boolean announceMsg, Message botMsg, User sender,
                                      boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, tracksRequestedByUsers, trackUrl, announceMsg, botMsg, true, addToBeginning);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackUrl, loader);
    }

    public void joinAudioChannel(TextChannel channel, AudioChannel vc, GuildMusicManager musicManager) {
        try {
            joinAudioChannel(vc, musicManager);
        } catch (InsufficientPermissionException e) {
            if (channel != null)
                channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(channel.getGuild(), RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS_TO_JOIN, Pair.of("{channel}", vc.getAsMention())).build())
                        .queue();
        }
    }

    public void joinAudioChannel(AudioChannel vc, GuildMusicManager musicManager) throws InsufficientPermissionException {
        if (vc.getMembers().size() == 0)
            throw new IllegalStateException("I can't join a voice channel with no one in it!");

        musicManager.getLink().connect(vc);
        musicManager.getScheduler().scheduleDisconnect(true);
    }

    public static String getRequester(Guild guild, AudioTrack track) {
        String requester = tracksRequestedByUsers.get(guild.getIdLong())
                .stream()
                .filter(trackInfo -> trackInfo.split(":")[1].equals(track.getIdentifier()))
                .findFirst()
                .orElse(null);
        requester = requester != null ? requester.split(":")[0] : null;
        return requester != null ? "<@" + requester + ">" : LocaleManager.getLocaleManager(guild).getMessage(RobertifyLocaleMessage.GeneralMessages.UNKNOWN_REQUESTER);
    }

    public static void removeRequester(Guild guild, AudioTrack track, User requester) {
        tracksRequestedByUsers.get(guild.getIdLong()).remove(requester.getId() + ":" + track.getIdentifier());
    }

    public static void clearRequesters(Guild guild) {
        tracksRequestedByUsers.remove(guild.getIdLong());
    }

    public static RobertifyAudioManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new RobertifyAudioManager();
        return INSTANCE;
    }
}
