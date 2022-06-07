package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.event.PlayerEventListenerAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import main.commands.slashcommands.commands.misc.PlaytimeCommand;
import main.constants.Toggles;
import main.exceptions.AutoPlayException;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.postgresql.tracks.TrackDB;
import main.utils.json.autoplay.AutoPlayConfig;
import main.utils.json.autoplay.AutoPlayUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.*;

public class TrackScheduler extends PlayerEventListenerAdapter {
    private final static HashMap<Guild, ConcurrentLinkedQueue<AudioTrack>> savedQueue = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final static HashMap<Long, ScheduledFuture<?>> disconnectExecutors = new HashMap<>();

    private final Guild guild;
    private final Link audioPlayer;
    @Setter @Getter
    private TextChannel announcementChannel = null;
    private AudioTrack lastPlayedTrackBuffer;
    @Getter
    private final static HashMap<Long, Stack<AudioTrack>> pastQueue = new HashMap<>();
    public ConcurrentLinkedQueue<AudioTrack> queue;
    public boolean repeating = false;
    public boolean playlistRepeating = false;
    private Message lastSentMsg = null;

    public TrackScheduler(Guild guild, Link audioPlayer) {
        this.guild = guild;
        this.audioPlayer = audioPlayer;
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public void queue(AudioTrack track) {
        if (getMusicPlayer().getPlayingTrack() != null) {
            queue.offer(track);
        } else {
            getMusicPlayer().playTrack(track);
        }
    }

    public void stop() {
        queue.clear();

        if (audioPlayer.getPlayer().getPlayingTrack() != null)
            audioPlayer.getPlayer().stopTrack();
    }

    @Override
    public void onTrackStart(IPlayer player, AudioTrack track) {
        if (disconnectExecutors.containsKey(guild.getIdLong())) {
            disconnectExecutors.get(guild.getIdLong()).cancel(true);
            disconnectExecutors.remove(guild.getIdLong());
        }

        lastPlayedTrackBuffer = track;

        if (repeating) return;

        ResumeUtils.getInstance().saveInfo(guild, guild.getSelfMember().getVoiceState().getChannel());

        if (!new TogglesConfig(guild).getToggle(Toggles.ANNOUNCE_MESSAGES)) return;

        if (RobertifyAudioManager.getUnannouncedTracks().contains(track.getIdentifier())) {
            RobertifyAudioManager.getUnannouncedTracks().remove(track.getIdentifier());
            return;
        }

        final var requester = RobertifyAudioManager.getRequester(guild, track);

        if (announcementChannel != null) {
            final var localeManager =LocaleManager.getLocaleManager(guild);
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(announcementChannel.getGuild(), localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_ANNOUNCEMENT_DESC, Pair.of("{title}", track.getInfo().title), Pair.of("{author}", track.getInfo().author))
                    + (new TogglesConfig(guild).getToggle(Toggles.SHOW_REQUESTER) ?
                    "\n\n" + localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER, Pair.of("{requester}", requester))
                    :
                    ""
            ));

            try {
                if (announcementChannel != null)
                    announcementChannel.sendMessageEmbeds(eb.build())
                            .queue(msg -> {
                                if (lastSentMsg != null)
                                    lastSentMsg.delete().queueAfter(3L, TimeUnit.SECONDS, null, new ErrorHandler()
                                            .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {}));
                                lastSentMsg = msg;
                            }, new ErrorHandler()
                                    .handle(ErrorResponse.MISSING_PERMISSIONS, e -> announcementChannel.sendMessage(eb.build().getDescription())
                                            .queue(nonEmbedMsg -> {
                                                if (lastSentMsg != null)
                                                    lastSentMsg.delete().queueAfter(3L, TimeUnit.SECONDS, null, new ErrorHandler()
                                                            .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {}));
                                            })
                                    ));
            } catch (InsufficientPermissionException ignored) {}
        }
    }

    @SneakyThrows
    public void nextTrack(AudioTrack lastTrack) {
        nextTrack(lastTrack, false, null);
    }

    public void nextTrack(AudioTrack lastTrack, boolean skipped, Long skippedAt) throws AutoPlayException {
        HashMap<Long, Long> playtime = PlaytimeCommand.playtime;

        if (lastTrack != null) {
            if (!skipped) {
                playtime.put(guild.getIdLong(), playtime.containsKey(guild.getIdLong()) ? playtime.get(guild.getIdLong()) + lastTrack.getInfo().length : lastTrack.getInfo().length);
            } else {
                playtime.put(guild.getIdLong(), playtime.containsKey(guild.getIdLong()) ? playtime.get(guild.getIdLong()) + skippedAt : skippedAt);
            }
        }

        if (queue.isEmpty())
            if (playlistRepeating)
                this.queue = new ConcurrentLinkedQueue<>(savedQueue.get(guild));

        AudioTrack nextTrack = queue.poll();

        if (getMusicPlayer().getPlayingTrack() != null)
            getMusicPlayer().stopTrack();

        try {
            if (nextTrack != null)
                getMusicPlayer().playTrack(nextTrack);
            else {
                if (lastTrack != null) {
                    if (new AutoPlayConfig(guild).getStatus()) {
                        switch (lastTrack.getSourceManager().getSourceName().toLowerCase()) {
                            case "youtube" -> AutoPlayUtils.loadRecommendedTracks(
                                    guild,
                                    announcementChannel,
                                    lastTrack
                            );
                            case "spotify" -> {
                                String youTubeID = TrackDB.getInstance().getSpotifyTable()
                                        .getTrackYouTubeID(lastTrack.getInfo().identifier);
                                AutoPlayUtils.loadRecommendedTracks(
                                        guild,
                                        announcementChannel,
                                        youTubeID
                                );
                            }
                            case "deezer" -> {
                                String youTubeID = TrackDB.getInstance().getDeezerTable()
                                        .getTrackYouTubeID(lastTrack.getInfo().identifier);
                                AutoPlayUtils.loadRecommendedTracks(
                                        guild,
                                        announcementChannel,
                                        youTubeID
                                );
                            }
                            default -> throw new AutoPlayException("This track can't be auto-played!");
                        }
                    } else scheduleDisconnect(true);
                } else scheduleDisconnect(true);
            }
        } catch (IllegalStateException e) {
            getMusicPlayer().playTrack(nextTrack);
        } catch (AutoPlayException e) {
            throw e;
        }

        final var dedicatedChannelConfig = new DedicatedChannelConfig(guild);
        if (dedicatedChannelConfig.isChannelSet())
            dedicatedChannelConfig.updateMessage();
    }

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        final var trackToUse = lastPlayedTrackBuffer;

        if (repeating) {
            if (trackToUse != null) {
                try {
                    AudioTrack clonedTrack = trackToUse.makeClone();
                    lastPlayedTrackBuffer = clonedTrack;
                    player.playTrack(clonedTrack);
                } catch (UnsupportedOperationException e) {
                    player.seekTo(0);
                }
            } else nextTrack(null);
        } else if (endReason.mayStartNext) {
            if (!pastQueue.containsKey(guild.getIdLong()))
                pastQueue.put(guild.getIdLong(), new Stack<>());
            pastQueue.get(guild.getIdLong()).push(trackToUse);
            nextTrack(trackToUse);
        }
    }

    @Override
    public void onTrackStuck(IPlayer player, AudioTrack track, long thresholdMs) {
        if (!new TogglesConfig(guild).getToggle(Toggles.ANNOUNCE_MESSAGES)) return;

        try {
            if (announcementChannel != null)
                announcementChannel.sendMessageEmbeds(
                                RobertifyEmbedUtils.embedMessage(
                                                guild,
                                                RobertifyLocaleMessage.TrackSchedulerMessages.TRACK_COULD_NOT_BE_PLAYED,
                                                Pair.of("{title}", track.getInfo().title),
                                                Pair.of("{author}", track.getInfo().author)
                                        )
                                        .build()
                        )
                        .queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES));
        } catch (InsufficientPermissionException ignored) {}

        nextTrack(track);
    }

    @Override
    public void onTrackException(IPlayer player, AudioTrack track, Exception exception) {
        if (exception.getMessage().contains("matching track")) {
            if (announcementChannel != null)
                announcementChannel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TrackSchedulerMessages.COULD_NOT_FIND_SOURCE).build())
                        .queue(msg -> msg.delete().queueAfter(10, TimeUnit.SECONDS));
        } else if (exception.getMessage().contains("copyright")) {
            if (announcementChannel != null)
                announcementChannel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TrackSchedulerMessages.COPYRIGHT_TRACK,
                            Pair.of("{title}", track.getInfo().title),
                            Pair.of("{author}", track.getInfo().author)
                        ).build())
                        .queue(msg -> msg.delete().queueAfter(10, TimeUnit.SECONDS));
        } else if (exception.getMessage().contains("unavailable")) {
            if (announcementChannel != null)
                announcementChannel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TrackSchedulerMessages.UNAVAILABLE_TRACK,
                                Pair.of("{title}", track.getInfo().title),
                                Pair.of("{author}", track.getInfo().author)
                        ).build())
                        .queue(msg -> msg.delete().queueAfter(10, TimeUnit.SECONDS));
        } else if (exception.getMessage().contains("playlist type is unviewable")) {
            if (announcementChannel != null)
                announcementChannel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TrackSchedulerMessages.UNVIEWABLE_PLAYLIST).build())
                        .queue(msg -> msg.delete().queueAfter(10, TimeUnit.SECONDS));
        } else {
            logger.error("There was an exception with playing the track.", exception);
        }
    }

    public void setSavedQueue(Guild guild, ConcurrentLinkedQueue<AudioTrack> queue) {
        ConcurrentLinkedQueue<AudioTrack> savedQueue = new ConcurrentLinkedQueue<>(queue);

        TrackScheduler.savedQueue.put(guild, savedQueue);
    }

    public void clearSavedQueue(Guild guild) {
        savedQueue.remove(guild);
    }

    public void addToBeginningOfQueue(AudioTrack track) {
        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        newQueue.offer(track);
        newQueue.addAll(queue);
        queue = newQueue;
    }

    public void addToBeginningOfQueue(List<AudioTrack> tracks) {
        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        tracks.forEach(newQueue::offer);
        newQueue.addAll(queue);
        queue = newQueue;
    }

    public void removeScheduledDisconnect(Guild guild) {
        if (disconnectExecutors.containsKey(guild.getIdLong())) {
            disconnectExecutors.get(guild.getIdLong()).cancel(false);
            disconnectExecutors.remove(guild.getIdLong());
            logger.debug("Removed scheduled player disconnect");
        }
    }

    public void scheduleDisconnect(boolean announceMsg) {
        logger.debug("Scheduling player disconnect in 5 minutes");
        scheduleDisconnect(announceMsg, 5, TimeUnit.MINUTES);
    }

    public void scheduleDisconnect(boolean announceMsg, long delay, TimeUnit timeUnit) {
        if (new GuildConfig(guild).get247())
            return;

        ScheduledFuture<?> schedule = executor.schedule(() -> disconnect(announceMsg), delay, timeUnit);

        if (disconnectExecutors.containsKey(guild.getIdLong())) {
            logger.debug("Scheduled disconnect already existed... Cancelling.");
            disconnectExecutors.get(guild.getIdLong()).cancel(false);
        }

        disconnectExecutors.put(guild.getIdLong(), schedule);
    }

    public void disconnect(boolean announceMsg) {
        final var channel = guild.getSelfMember().getVoiceState().getChannel();

        if (RobertifyAudioManager.getInstance().getMusicManager(guild).getPlayer().getPlayingTrack() != null)
            return;

        if (!new GuildConfig(guild).get247()) {
            if (channel != null) {
                RobertifyAudioManager.getInstance().getMusicManager(guild)
                        .leave();
                disconnectExecutors.remove(guild.getIdLong());
                logger.debug("Removed scheduled disconnect from mapping");

                if (announceMsg && announcementChannel != null)
                    announcementChannel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TrackSchedulerMessages.INACTIVITY_LEAVE, Pair.of("{channel}", channel.getAsMention())).build())
                            .queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES));
            }
        }
    }

    public void removeSavedQueue(Guild guild) {
        savedQueue.remove(guild);
    }

    public IPlayer getMusicPlayer() {
        return audioPlayer.getPlayer();
    }
}
