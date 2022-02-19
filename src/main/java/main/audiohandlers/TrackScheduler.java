package main.audiohandlers;

import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.event.PlayerEventListenerAdapter;
import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackEndReason;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import main.commands.commands.misc.PlaytimeCommand;
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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    @Getter
    private final Stack<AudioTrack> pastQueue;
    public ConcurrentLinkedQueue<AudioTrack> queue;
    public boolean repeating = false;
    public boolean playlistRepeating = false;
    private Message lastSentMsg = null;

    public TrackScheduler(Guild guild, Link audioPlayer) {
        this.guild = guild;
        this.audioPlayer = audioPlayer;
        this.queue = new ConcurrentLinkedQueue<>();
        this.pastQueue = new Stack<>();
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

        if (repeating) return;

        if (!new TogglesConfig().getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) return;

        if (RobertifyAudioManager.getUnannouncedTracks().contains(track.getTrack())) {
            RobertifyAudioManager.getUnannouncedTracks().remove(track.getTrack());
            return;
        }

        final var requester = RobertifyAudioManager.getRequester(guild, track);

        if (announcementChannel != null) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(announcementChannel.getGuild(), "Now Playing: `" + track.getInfo().getTitle() + "` by `"+track.getInfo().getAuthor() +"`"
                    + (new TogglesConfig().getToggle(guild, Toggles.SHOW_REQUESTER) ?
                    "\n\n~ Requested by " + requester
                    :
                    ""
            ));

            try {
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
            } catch (NullPointerException e) {
                new GuildConfig().setAnnouncementChannelID(guild.getIdLong(), -1L);
            } catch (InsufficientPermissionException ignored) {}
        }
    }

    @SneakyThrows
    public void nextTrack(AudioTrack lastTrack) {
        nextTrack(lastTrack, false, null);
    }

    public void nextTrack(AudioTrack lastTrack, boolean skipped, Long skippedAt) throws AutoPlayException {
        HashMap<Long, Long> playtime = PlaytimeCommand.playtime;
        if (!skipped) {
            playtime.put(guild.getIdLong(), playtime.containsKey(guild.getIdLong()) ? playtime.get(guild.getIdLong()) + pastQueue.peek().getInfo().getLength() : pastQueue.peek().getInfo().getLength());
        } else {
            playtime.put(guild.getIdLong(), playtime.containsKey(guild.getIdLong()) ? playtime.get(guild.getIdLong()) + skippedAt : skippedAt);
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
                    if (new AutoPlayConfig().getStatus(guild.getIdLong())) {
                        switch (lastTrack.getInfo().getSourceName().toLowerCase()) {
                            case "youtube" -> AutoPlayUtils.loadRecommendedTracks(
                                    guild,
                                    announcementChannel,
                                    lastTrack
                            );
                            case "spotify" -> {
                                String youTubeID = TrackDB.getInstance().getSpotifyTable()
                                        .getTrackYouTubeID(lastTrack.getInfo().getIdentifier());
                                AutoPlayUtils.loadRecommendedTracks(
                                        guild,
                                        announcementChannel,
                                        youTubeID
                                );
                            }
                            case "deezer" -> {
                                String youTubeID = TrackDB.getInstance().getDeezerTable()
                                        .getTrackYouTubeID(lastTrack.getInfo().getIdentifier());
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

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);
    }

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (repeating) {
            if (track != null) {
                try {
                    player.playTrack(track);
                } catch (UnsupportedOperationException e) {
                    player.seekTo(0);
                }
            } else nextTrack(null);
        } else if (endReason.mayStartNext) {
            pastQueue.push(track);
            nextTrack(track);
        }
    }

    @Override
    public void onTrackStuck(IPlayer player, AudioTrack track, long thresholdMs) {
        if (!new TogglesConfig().getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) return;

        if (!new GuildConfig().announcementChannelIsSet(guild.getIdLong())) return;

        TextChannel announcementChannel = Robertify.api.getTextChannelById(new GuildConfig().getAnnouncementChannelID(this.guild.getIdLong()));
        try {
            announcementChannel.sendMessageEmbeds(
                            RobertifyEmbedUtils.embedMessage(
                                            guild,
                                            "`" + track.getInfo().getTitle() + "` by `" + track.getInfo().getAuthor() + "` could not be played!\nSkipped to the next song. (If available)")
                                    .build()
                    )
                    .queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES));
        } catch (NullPointerException e) {
            new GuildConfig().setAnnouncementChannelID(guild.getIdLong(), -1L);
        } catch (InsufficientPermissionException ignored) {}

        nextTrack(track);
    }

    @Override
    public void onTrackException(IPlayer player, AudioTrack track, Exception exception) {
        logger.error("There was an exception with playing the track.", exception);
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
        }
    }

    public void scheduleDisconnect(boolean announceMsg) {
        scheduleDisconnect(announceMsg, 5, TimeUnit.MINUTES);
    }

    public void scheduleDisconnect(boolean announceMsg, long delay, TimeUnit timeUnit) {
        if (new GuildConfig().get247(guild.getIdLong()))
            return;

        ScheduledFuture<?> schedule = executor.schedule(() -> {
            final var channel = guild.getSelfMember().getVoiceState().getChannel();

            if (RobertifyAudioManager.getInstance().getMusicManager(guild).getPlayer().getPlayingTrack() != null)
                return;

            if (!new GuildConfig().get247(guild.getIdLong())) {
                if (channel != null) {
                    RobertifyAudioManager.getInstance().getMusicManager(guild)
                                    .leave();
                    disconnectExecutors.remove(guild.getIdLong());

                    final var guildConfig = new GuildConfig();

                    if (guildConfig.announcementChannelIsSet(guild.getIdLong()) && announceMsg)
                        Robertify.api.getTextChannelById(guildConfig.getAnnouncementChannelID(guild.getIdLong()))
                                .sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I have left " + channel.getAsMention() + " due to inactivity.").build())
                                .queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES));
                }
            }
        }, delay, timeUnit);

        if (disconnectExecutors.containsKey(guild.getIdLong()))
            disconnectExecutors.get(guild.getIdLong()).cancel(false);

        disconnectExecutors.put(guild.getIdLong(), schedule);
    }

    public void removeSavedQueue(Guild guild) {
        savedQueue.remove(guild);
    }

    public IPlayer getMusicPlayer() {
        return audioPlayer.getPlayer();
    }
}
