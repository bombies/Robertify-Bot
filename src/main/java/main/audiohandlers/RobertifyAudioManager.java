package main.audiohandlers;

import com.google.inject.*;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
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
import main.audiohandlers.spotify.SpotifyAudioSourceManager;
import main.commands.CommandContext;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.*;

public class RobertifyAudioManager extends AbstractModule {
    private static RobertifyAudioManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    @Getter
    private static final HashMap<AudioTrack, User> trackRequestedByUser = new HashMap<>();
    @Getter
    private final AudioPlayerManager audioPlayerManager;

    public RobertifyAudioManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        audioPlayerManager.registerSourceManager(new SpotifyAudioSourceManager(new YoutubeAudioSourceManager()));

        audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
        audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        audioPlayerManager.registerSourceManager(new BandcampAudioSourceManager());
        audioPlayerManager.registerSourceManager(new VimeoAudioSourceManager());
        audioPlayerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new BeamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new HttpAudioSourceManager());
        audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());

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
    public void loadAndPlay(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx, Message botMsg) {
        final GuildMusicManager musicManager = getMusicManager(channel.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(ctx.getChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }
        loadTrack(trackUrl, musicManager, ctx, new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES), botMsg);
    }

    public void loadAndPlay(String trackUrl, TextChannel announcementChannel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, Message botMsg, SlashCommandEvent event) {
        final GuildMusicManager musicManager = getMusicManager(memberVoiceState.getGuild());

        if (trackUrl.contains("ytsearch:") && !trackUrl.endsWith("audio"))
            trackUrl += " audio";

        try {
            joinVoiceChannel(event.getTextChannel(), selfVoiceState, memberVoiceState);
        } catch (Exception e) {
            return;
        }
        loadTrack(trackUrl, musicManager, new TogglesConfig().getToggle(selfVoiceState.getGuild(), Toggles.ANNOUNCE_MESSAGES), botMsg, event.getUser());
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           CommandContext ctx, boolean announceMsg, Message botMsg) {

        final AudioLoader loader = new AudioLoader(ctx.getAuthor(), musicManager, trackRequestedByUser, trackUrl, announceMsg, botMsg);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    private void loadTrack(String trackUrl, GuildMusicManager musicManager,
                           boolean announceMsg, Message botMsg, User sender) {

        final AudioLoader loader = new AudioLoader(sender, musicManager, trackRequestedByUser, trackUrl, announceMsg, botMsg);
        final var audioRef = new RobertifyAudioReference(trackUrl, null);
        this.audioPlayerManager.loadItemOrdered(musicManager, audioRef, loader);
    }

    public void joinVoiceChannel(TextChannel channel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        if (!selfVoiceState.inVoiceChannel()) {
            AudioManager audioManager = selfVoiceState.getGuild().getAudioManager();

            try {
                audioManager.openAudioConnection(memberVoiceState.getChannel());
            } catch (InsufficientPermissionException e) {
                channel.sendMessageEmbeds(EmbedUtils.embedMessage("I do not have enough permissions to join " + memberVoiceState.getChannel().getAsMention()).build())
                        .queue();
                throw e;
            }
            audioManager.setSelfDeafened(true);
        }
    }

    public static User getRequester(AudioTrack track) {
        return trackRequestedByUser.get(track);
    }

    public static void removeRequester(AudioTrack track, User requester) {
        trackRequestedByUser.remove(track, requester);
    }

    public static RobertifyAudioManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new RobertifyAudioManager();
        return INSTANCE;
    }
}
