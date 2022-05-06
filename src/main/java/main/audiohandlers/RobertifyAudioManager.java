package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.loaders.AudioLoader;
import main.audiohandlers.loaders.AutoPlayLoader;
import main.audiohandlers.loaders.SearchResultLoader;
import main.audiohandlers.sources.applemusic.AppleMusicSourceManager;
import main.audiohandlers.sources.deezer.DeezerSourceManager;
import main.audiohandlers.sources.resume.ResumeSourceManager;
import main.audiohandlers.sources.spotify.SpotifySourceManager;
import main.commands.prefixcommands.CommandContext;
import main.constants.ENV;
import main.constants.Toggles;
import main.main.Config;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.toggles.TogglesConfig;
import main.utils.resume.ResumeData;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;

public class RobertifyAudioManager {
    private static final Logger logger = LoggerFactory.getLogger(RobertifyAudioManager.class);

    private static RobertifyAudioManager INSTANCE;
    @Getter
    private final Map<Long, GuildMusicManager> musicManagers;
    @Getter
    /*
    Each guild will have a list that consists of tracks formatted "userid:trackstring"
     */
    private static final HashMap<Long, List<String>> tracksRequestedByUsers = new HashMap<>();
    @Getter
    private static final List<String> unannouncedTracks = new ArrayList<>();
    @Getter
    private final AudioPlayerManager playerManager;

    private RobertifyAudioManager() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager(true);

        YoutubeHttpContextFilter.setPAPISID(Config.get(ENV.PAPISID));
        YoutubeHttpContextFilter.setPSID(Config.get(ENV.PSID));

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

        this.playerManager.registerSourceManager(new SpotifySourceManager(playerManager));
        this.playerManager.registerSourceManager(new AppleMusicSourceManager(playerManager));
        this.playerManager.registerSourceManager(new DeezerSourceManager(playerManager));
        this.playerManager.registerSourceManager(new ResumeSourceManager(playerManager));
        this.playerManager.registerSourceManager(youtubeAudioSourceManager);
        this.playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        this.playerManager.registerSourceManager(new BeamAudioSourceManager());
        this.playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        this.playerManager.registerSourceManager(new BandcampAudioSourceManager());
        this.playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        this.playerManager.registerSourceManager(new VimeoAudioSourceManager());
        this.playerManager.registerSourceManager(new HttpAudioSourceManager());
        this.playerManager.registerSourceManager(new LocalAudioSourceManager());
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (gid) -> new GuildMusicManager(guild));
    }

    public void removeMusicManager(Guild guild) {
        this.musicManagers.get(guild.getIdLong()).destroy();
        this.musicManagers.remove(guild.getIdLong());
    }

    @SneakyThrows
    public void loadAndPlay(long gid, long channelID, ResumeData.GuildResumeData track) {
        final var guild = Robertify.getShardManager().getGuildById(gid);
        final var musicManager = getMusicManager(guild);

        try {
            joinVoiceChannel(null, guild.getVoiceChannelById(channelID), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(track, musicManager);
    }

    @SneakyThrows
    public void loadAndPlay(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState,
                            Message botMsg, boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            joinVoiceChannel(channel, (VoiceChannel) memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                memberVoiceState.getMember().getUser(),
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                addToBeginning
        );
    }

    @SneakyThrows
    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, TextChannel channel,
                            User user, Message botMsg, boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            joinVoiceChannel(channel, (VoiceChannel) memberVoiceState.getChannel(), musicManager);
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
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(ctx.getChannel(), (VoiceChannel) memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadPlaylistShuffled(
                memberVoiceState.getMember().getUser(),
                trackUrl,
                musicManager,
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                addToBeginning
        );
    }

    @SneakyThrows
    public void loadAndPlayFromDedicatedChannel(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
                                                GuildVoiceState memberVoiceState, Message botMsg,
                                                boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            joinVoiceChannel(channel, (VoiceChannel) memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadTrack(
                trackUrl,
                musicManager,
                memberVoiceState.getMember().getUser(),
                false,
                botMsg,
                addToBeginning
        );
    }

    @SneakyThrows
    public void loadAndPlayFromDedicatedChannelShuffled(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState,
                                                GuildVoiceState memberVoiceState, Message botMsg,
                                                boolean addToBeginning) {

        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            joinVoiceChannel(channel, (VoiceChannel) memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }

        loadPlaylistShuffled(
                memberVoiceState.getMember().getUser(),
                trackUrl,
                musicManager,
                false,
                botMsg,
                addToBeginning
        );
    }

    public void loadAndPlay(String trackUrl, GuildVoiceState selfVoiceState,
                            GuildVoiceState memberVoiceState, Message botMsg,
                            SlashCommandEvent event, boolean addToBeginning) {
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            joinVoiceChannel(event.getTextChannel(), (VoiceChannel) memberVoiceState.getChannel(), musicManager);
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
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            joinVoiceChannel(event.getTextChannel(), (VoiceChannel) memberVoiceState.getChannel(), musicManager);
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
        final var musicManager = getMusicManager(memberVoiceState.getGuild());

        try {
            joinVoiceChannel(event.getTextChannel(), (VoiceChannel) memberVoiceState.getChannel(), musicManager);
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
                                 GuildVoiceState memberVoiceState, Message botMsg,
                                 boolean addToBeginning) {
        final var musicManager = getMusicManager(channel.getGuild());

        try {
            joinVoiceChannel(channel, (VoiceChannel) memberVoiceState.getChannel(), musicManager);
        } catch (Exception e) {
            return;
        }
        loadTrack(
                path,
                musicManager,
                memberVoiceState.getMember().getUser(),
                new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES),
                botMsg,
                addToBeginning
        );
    }

    private void loadTrack(ResumeData.GuildResumeData track, GuildMusicManager musicManager) {
        final AudioLoader loader = new AudioLoader(musicManager.getGuild().getSelfMember().getUser(), musicManager, tracksRequestedByUsers, track.getGuildObject().toString(), false, null, false, false);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, ResumeSourceManager.SEARCH_PREFIX + track.getGuildObject().toString(), loader);
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

    public void loadRecommendedTracks(GuildMusicManager musicManager, TextChannel channel, String query) {
        final AutoPlayLoader loader = new AutoPlayLoader(musicManager, channel);
        musicManager.getPlayerManager().loadItemOrdered(musicManager, query, loader);
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

    public void joinVoiceChannel(TextChannel channel, VoiceChannel vc, GuildMusicManager musicManager) {
        if (vc.getMembers().size() == 0)
            throw new IllegalStateException("I can't join a voice channel with no one in it!");

        try {
            musicManager.getLink().connect(vc);
            musicManager.getScheduler().scheduleDisconnect(true);
        } catch (InsufficientPermissionException e) {
            if (channel != null)
                channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(channel.getGuild(), "I do not have enough permissions to join " + vc.getAsMention()).build())
                        .queue();
            throw e;
        }
    }

    public static String getRequester(Guild guild, AudioTrack track) {
        return "<@" +
                tracksRequestedByUsers.get(guild.getIdLong())
                        .stream()
                        .filter(trackInfo -> trackInfo.split(":")[1].equals(track.getIdentifier()))
                        .findFirst().get()
                        .split(":")[0]
                + ">";
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
