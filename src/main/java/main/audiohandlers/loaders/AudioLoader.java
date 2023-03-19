package main.audiohandlers.loaders;

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AudioLoader implements AudioLoadResultHandler {
    private final Logger logger = LoggerFactory.getLogger(AudioLoader.class);

    private final Guild guild;
    private final User sender;
    private final GuildMusicManager musicManager;
    private final boolean announceMsg;
    private final HashMap<Long, List<String>> trackRequestedByUser;
    private final String trackUrl;
    private final Message botMsg;
    private final boolean loadPlaylistShuffled;
    private final boolean addToBeginning;
    private final GuildMessageChannel announcementChannel;
    private final RequestChannelConfig requestChannelConfig;

    public AudioLoader(@NotNull User sender, GuildMusicManager musicManager, HashMap<Long, List<String>> trackRequestedByUser,
                       String trackUrl, boolean announceMsg, Message botMsg, boolean loadPlaylistShuffled, boolean addToBeginning) {

        this.guild = musicManager.getGuild();
        this.sender = sender;
        this.musicManager = musicManager;
        this.trackRequestedByUser = trackRequestedByUser;
        this.trackUrl = trackUrl;
        this.announceMsg = announceMsg;
        this.botMsg = botMsg;
        this.loadPlaylistShuffled = loadPlaylistShuffled;
        this.addToBeginning = addToBeginning;
        this.announcementChannel = botMsg != null ? botMsg.getChannel().asGuildMessageChannel() : null;
        this.requestChannelConfig = new RequestChannelConfig(this.guild);
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        sendTrackLoadedMessage(audioTrack);

        if (!announceMsg)
            RobertifyAudioManager.getUnannouncedTracks().add(audioTrack.getIdentifier());


        trackRequestedByUser.putIfAbsent(guild.getIdLong(), new ArrayList<>());
        trackRequestedByUser.get(guild.getIdLong()).add(sender.getId() + ":" + audioTrack.getIdentifier());

        final var scheduler = musicManager.getScheduler();
        scheduler.setAnnouncementChannel(announcementChannel);

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(audioTrack);
        else
            scheduler.queue(audioTrack);

        AudioTrackInfo info = audioTrack.getInfo();
        new LogUtils(guild).sendLog(LogType.QUEUE_ADD, RobertifyLocaleMessage.AudioLoaderMessages.QUEUE_ADD_LOG,
                Pair.of("{user}", sender.getAsMention()),
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author)
        );

        if (scheduler.isPlaylistRepeating())
            scheduler.setSavedQueue(scheduler.getQueue());

        if (requestChannelConfig.isChannelSet())
            requestChannelConfig.updateMessage();

    }

    private void sendTrackLoadedMessage(AudioTrack audioTrack) {
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.AudioLoaderMessages.QUEUE_ADD,
                Pair.of("{title}", audioTrack.getInfo().title),
                Pair.of("{author}", audioTrack.getInfo().author)
        );

        if (botMsg != null) {
            botMsg.editMessageEmbeds(eb.build())
                    .queue(success -> success.editMessageComponents().queue(msg -> {
                        if (requestChannelConfig.isChannelSet())
                            if (requestChannelConfig.getChannelID() == msg.getChannel().getIdLong())
                                msg.delete().queueAfter(10, TimeUnit.SECONDS, null, new ErrorHandler()
                                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                                );
                    }));
        } else {
            if (requestChannelConfig.isChannelSet())
                requestChannelConfig.getTextChannel()
                        .sendMessageEmbeds(eb.build())
                        .queue(msg -> msg.delete().queueAfter(
                                10,
                                TimeUnit.SECONDS,
                                null,
                                new ErrorHandler()
                                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                        ));
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        List<AudioTrack> tracks = audioPlaylist.getTracks();

        final var dedicatedChannelConfig = new RequestChannelConfig(guild);
        trackRequestedByUser.putIfAbsent(guild.getIdLong(), new ArrayList<>());

        if (audioPlaylist.isSearchResult()) {
            sendTrackLoadedMessage(tracks.get(0));

            if (!announceMsg)
                RobertifyAudioManager.getUnannouncedTracks().add(tracks.get(0).getIdentifier());

            if (sender != null)
                trackRequestedByUser.get(guild.getIdLong()).add(sender.getId() + ":" + tracks.get(0).getIdentifier());

            final var scheduler = musicManager.getScheduler();
            scheduler.setAnnouncementChannel(announcementChannel);

            if (addToBeginning)
                scheduler.addToBeginningOfQueue(tracks.get(0));
            else
                scheduler.queue(tracks.get(0));

            AudioTrackInfo info = tracks.get(0).getInfo();
            if (sender != null)
                new LogUtils(guild).sendLog(LogType.QUEUE_ADD, RobertifyLocaleMessage.AudioLoaderMessages.QUEUE_ADD_LOG,
                        Pair.of("{user}", sender.getAsMention()),
                        Pair.of("{title}", info.title),
                        Pair.of("{author}", info.author)
                );

            if (scheduler.isPlaylistRepeating())
                scheduler.setSavedQueue(scheduler.getQueue());

        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
                    Pair.of("{numTracks}", String.valueOf(tracks.size())),
                    Pair.of("{playlist}", audioPlaylist.getName())
            );

            if (botMsg != null)
                botMsg.editMessageEmbeds(eb.build()).queue(msg -> {
                    if (dedicatedChannelConfig.isChannelSet())
                        if (dedicatedChannelConfig.getChannelID() == msg.getChannel().getIdLong())
                            msg.delete().queueAfter(
                                    10,
                                    TimeUnit.SECONDS,
                                    null,
                                    new ErrorHandler()
                                            .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                            );
                });
            else {
                if (dedicatedChannelConfig.isChannelSet())
                    dedicatedChannelConfig.getTextChannel()
                            .sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(10, TimeUnit.SECONDS));
            }

            if (!announceMsg)
                for (final AudioTrack track : tracks)
                    RobertifyAudioManager.getUnannouncedTracks().add(track.getIdentifier());

            if (loadPlaylistShuffled)
                Collections.shuffle(tracks);

            final var scheduler = musicManager.getScheduler();
            scheduler.setAnnouncementChannel(announcementChannel);

            if (addToBeginning)
                scheduler.addToBeginningOfQueue(tracks);

            if (sender != null) {
                new LogUtils(guild).sendLog(LogType.QUEUE_ADD, RobertifyLocaleMessage.AudioLoaderMessages.QUEUE_PLAYLIST_ADD_LOG,
                        Pair.of("{user}", sender.getAsMention()),
                        Pair.of("{numTracks}", String.valueOf(tracks.size())),
                        Pair.of("{playlist}", audioPlaylist.getName())
                );
            }

            for (final var track : tracks) {
                if (sender != null)
                    trackRequestedByUser.get(guild.getIdLong()).add(sender.getId() + ":" + track.getIdentifier());

                if (!addToBeginning)
                    scheduler.queue(track);
            }

            if (scheduler.isPlaylistRepeating())
                scheduler.setSavedQueue(scheduler.getQueue());

        }
        if (dedicatedChannelConfig.isChannelSet())
            dedicatedChannelConfig.updateMessage();
    }

    @Override
    public void noMatches() {
        EmbedBuilder eb = (trackUrl.length() < 4096) ? RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.AudioLoaderMessages.NO_TRACK_FOUND, Pair.of("{query}", trackUrl.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, "")))
                : RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.AudioLoaderMessages.NO_TRACK_FOUND_ALT);
        if (botMsg != null)
            botMsg.editMessageEmbeds(eb.build()).queue(msg -> {
                if (requestChannelConfig.isChannelSet())
                    if (requestChannelConfig.getChannelID() == msg.getChannel().getIdLong())
                        msg.delete().queueAfter(
                                10,
                                TimeUnit.SECONDS,
                                null,
                                new ErrorHandler()
                                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                        );
            });
        else {
            new RequestChannelConfig(guild).getTextChannel()
                    .sendMessageEmbeds(eb.build())
                    .queue(msg -> msg.delete().queueAfter(
                            10,
                            TimeUnit.SECONDS,
                            null,
                            new ErrorHandler()
                                    .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                    ));
        }

        if (musicManager.getScheduler().getQueue().isEmpty() && musicManager.getPlayer().getPlayingTrack() == null)
            musicManager.getScheduler().scheduleDisconnect(false, 1, TimeUnit.SECONDS);
    }

    @Override
    public void loadFailed(FriendlyException e) {
        if (musicManager.getPlayer().getPlayingTrack() == null)
            musicManager.getGuild().getAudioManager().closeAudioConnection();

        if (!e.getMessage().contains("available") && !e.getMessage().contains("format"))
            logger.error("[FATAL ERROR] Could not load track!", e);

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild,
                e.getMessage().contains("available") ? e.getMessage()
                        : e.getMessage().contains("format") ? e.getMessage() :
                        LocaleManager.getLocaleManager(guild).getMessage(RobertifyLocaleMessage.AudioLoaderMessages.ERROR_LOADING_TRACK)
        );
        if (botMsg != null)
            botMsg.editMessageEmbeds(eb.build()).queue(msg -> {
                if (requestChannelConfig.isChannelSet())
                    if (requestChannelConfig.getChannelID() == msg.getChannel().getIdLong())
                        msg.delete().queueAfter(
                                10,
                                TimeUnit.SECONDS,
                                null,
                                new ErrorHandler()
                                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                        );
            });
        else {
            new RequestChannelConfig(guild).getTextChannel()
                    .sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(
                            10,
                            TimeUnit.SECONDS,
                            null,
                            new ErrorHandler()
                                    .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                    ));
        }
    }
}
