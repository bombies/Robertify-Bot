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
import main.audiohandlers.sources.resume.ResumeSourceManager;
import main.commands.prefixcommands.CommandContext;
import main.constants.ENV;
import main.constants.Toggles;
import main.main.Config;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.resume.ResumableTrack;
import main.utils.resume.ResumeData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class RobertifyAudioManager {
    private static final Logger logger = LoggerFactory.getLogger(RobertifyAudioManager.class);

    private static RobertifyAudioManager INSTANCE;

    @Getter
    private static final Map<Long, GuildMusicManager> musicManagers = new ConcurrentSkipListMap<>();

    @Getter
    private static final List<String> unannouncedTracks = new ArrayList<>();

    @Getter
    private final AudioPlayerManager playerManager;

    private RobertifyAudioManager() {
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
        this.playerManager.registerSourceManager(new ResumeSourceManager(this.playerManager));
        this.playerManager.registerSourceManager(new SpotifySourceManager(Config.getProviders(), Config.get(ENV.SPOTIFY_CLIENT_ID), Config.get(ENV.SPOTIFY_CLIENT_SECRET), "us", this.playerManager));
        this.playerManager.registerSourceManager(new AppleMusicSourceManager(Config.getProviders(), null, "us", this.playerManager));
        this.playerManager.registerSourceManager(new DeezerAudioSourceManager(Config.get(ENV.DEEZER_ACCESS_TOKEN)));
        AudioSourceManagers.registerRemoteSources(this.playerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), (gid) -> {
            logger.debug("Creating new music manager for {}", guild.getName());
            return new GuildMusicManager(guild);
        });
    }

    public void removeMusicManager(Guild guild) {
        musicManagers.get(guild.getIdLong()).destroy();
        musicManagers.remove(guild.getIdLong());
    }

    @SneakyThrows
    public void loadAndPlay(GuildMessageChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
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
                            TogglesConfig.getConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
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
                            GuildVoiceState memberVoiceState, GuildMessageChannel channel,
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
                            TogglesConfig.getConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
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
                            TogglesConfig.getConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    @SneakyThrows
    public void loadAndPlayFromDedicatedChannel(GuildMessageChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
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
    public void loadAndPlayFromDedicatedChannelShuffled(GuildMessageChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
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
                    joinAudioChannel(event.getChannel().asGuildMessageChannel(), voiceChannel, musicManager);
                    loadTrack(
                            trackUrl,
                            musicManager,
                            TogglesConfig.getConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
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
                    joinAudioChannel(event.getChannel().asGuildMessageChannel(), voiceChannel, musicManager);
                    loadPlaylistShuffled(
                            trackUrl,
                            musicManager,
                            TogglesConfig.getConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
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
                    joinAudioChannel(event.getChannel().asGuildMessageChannel(), voiceChannel, musicManager);
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

    public void loadAndPlayLocal(GuildMessageChannel channel, String path, GuildVoiceState selfVoiceState,
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
                            TogglesConfig.getConfig(selfVoiceState.getGuild()).getToggle(Toggles.ANNOUNCE_MESSAGES),
                            botMsg,
                            addToBeginning
                    );
                }
        } catch (Exception e) {
            logger.info("An unexpected error occurred!", e);
        }
    }

    public void loadAndResume(GuildMusicManager musicManager, ResumeData data) {
        final var channelId = data.getChannel_id();
        final var voiceChannel = Robertify.getShardManager().getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            logger.warn("There was resume data for a guild but the voice channel is invalid! Data: {}", data);
            return;
        }

        if (voiceChannel.getMembers().size() == 0)
            return;

        joinAudioChannel(voiceChannel, musicManager);
        resumeTracks(data.getTracks(), musicManager.getScheduler().getAnnouncementChannel(),  musicManager);
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           User user, boolean announceMsg, Message botMsg,
                           boolean addToBeginning) {
        final AudioLoader loader = new AudioLoader(user, musicManager, trackUrl, announceMsg, botMsg, false, addToBeginning);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackUrl, loader);
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           boolean announceMsg, Message botMsg, User sender,
                           boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, trackUrl, announceMsg, botMsg, false, addToBeginning);
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

    public void loadRecommendedTracks(GuildMusicManager musicManager, GuildMessageChannel channel, String trackIds) {
        final AutoPlayLoader loader = new AutoPlayLoader(musicManager, channel);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, String.format("%sseed_tracks=%s&limit=%d", SpotifySourceManager.RECOMMENDATIONS_PREFIX, trackIds, 30), loader);
    }

    public void loadRecommendedTracks(Guild guild, GuildMessageChannel channel, String trackIds) {
        loadRecommendedTracks(getMusicManager(guild), channel, trackIds);
    }

    private void loadPlaylistShuffled(User requester, String trackUrl, GuildMusicManager musicManager, boolean announceMsg, Message botMsg,
                                      boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(requester, musicManager, trackUrl, announceMsg, botMsg, true, addToBeginning);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackUrl, loader);
    }

    private void loadPlaylistShuffled(String trackUrl, GuildMusicManager musicManager,
                                      boolean announceMsg, Message botMsg, User sender,
                                      boolean addToBeginning) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, trackUrl, announceMsg, botMsg, true, addToBeginning);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackUrl, loader);
    }

    private void resumeTracks(Collection<ResumableTrack> trackList, GuildMessageChannel announcementChannel, GuildMusicManager musicManager) {
        trackList.forEach(track -> {
            final var requester = track.getRequester();
            musicManager.getScheduler().addRequester(requester.getId(), requester.getTrackId());
        });

        final var trackURL = ResumeSourceManager.SEARCH_PREFIX + ResumableTrack.collectionToString(trackList);
        final var loader = new AudioLoader(
                null,
                musicManager,
                trackURL,
                true,
                null,
                announcementChannel,
                false,
                false
        );
        musicManager.getPlayerManager().loadItemOrdered(musicManager, trackURL, loader);
    }

    public void joinAudioChannel(GuildMessageChannel channel, AudioChannel vc, GuildMusicManager musicManager) {
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

    public static String getRequesterAsMention(Guild guild, AudioTrack track) {
        final var requester = getInstance().getMusicManager(guild)
                .getScheduler()
                .findRequester(track.getIdentifier());
        final var requesterId = requester != null ? requester.getId() : null;
        return requesterId != null ? "<@" + requesterId + ">" : LocaleManager.getLocaleManager(guild).getMessage(RobertifyLocaleMessage.GeneralMessages.UNKNOWN_REQUESTER);
    }

    public static void clearRequesters(Guild guild) {
        getInstance().getMusicManager(guild)
                .getScheduler()
                .clearRequesters();
    }

    public static RobertifyAudioManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new RobertifyAudioManager();
        return INSTANCE;
    }
}
