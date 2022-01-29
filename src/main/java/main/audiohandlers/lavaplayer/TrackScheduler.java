package main.audiohandlers.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import main.audiohandlers.AbstractTrackScheduler;
import main.audiohandlers.RobertifyAudioManager;
import main.constants.Toggles;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
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
import java.util.Stack;
import java.util.concurrent.*;

public class TrackScheduler extends AudioEventAdapter implements AbstractTrackScheduler {

    private final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final static HashMap<Long, ScheduledFuture<?>> disconnectExecutors = new HashMap<>();

    public final AudioPlayer player;
    private final static HashMap<Guild, ConcurrentLinkedQueue<AudioTrack>> savedQueue = new HashMap<>();
    @Getter
    private final Stack<AudioTrack> pastQueue;
    public ConcurrentLinkedQueue<AudioTrack> queue;
    public boolean repeating = false;
    public boolean playlistRepeating = false;
    private Message lastSentMsg = null;
    @Getter
    private final Guild guild;

    public TrackScheduler(AudioPlayer player, Guild guild) {
        this.player = player;
        this.queue = new ConcurrentLinkedQueue<>();
        this.pastQueue = new Stack<>();
        this.guild = guild;
    }

    public void queue(AudioTrack track) {
        if (!this.player.startTrack(track, true)) {
            this.queue.offer(track);
        }
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if (disconnectExecutors.containsKey(guild.getIdLong())) {
            disconnectExecutors.get(guild.getIdLong()).cancel(false);
            disconnectExecutors.remove(guild.getIdLong());
        }

        if (!repeating) {
            if (!new TogglesConfig().getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) return;

            if (!new GuildConfig().announcementChannelIsSet(guild.getIdLong())) return;

            if (RobertifyAudioManager.getUnannouncedTracks().contains(track)) {
                RobertifyAudioManager.getUnannouncedTracks().remove(track);
                return;
            }

            final var requester = RobertifyAudioManager.getRequester(track);
            TextChannel announcementChannel = Robertify.api.getTextChannelById(new GuildConfig().getAnnouncementChannelID(this.guild.getIdLong()));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(announcementChannel.getGuild(), "Now Playing: `" + track.getInfo().title + "` by `"+track.getInfo().author+"`"
                    + ((new TogglesConfig().getToggle(guild, Toggles.SHOW_REQUESTER) && requester != null) ?
                    "\n\n~ Requested by " + requester.getAsMention()
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
                                        }, new ErrorHandler()
                                                .handle(ErrorResponse.MISSING_PERMISSIONS, ignored -> {}))
                        ));
            } catch (NullPointerException e) {
                new GuildConfig().setAnnouncementChannelID(guild.getIdLong(), -1L);
            } catch (InsufficientPermissionException ignored) {}
        }
    }

    public void nextTrack() {
        if (this.queue.isEmpty()) {
            if (playlistRepeating)
                this.queue = new ConcurrentLinkedQueue<>(savedQueue.get(this.guild));
            else {
                scheduleDisconnect(true);
            }
        }

        AudioTrack nextTrack = this.queue.poll();

        if (nextTrack != null)
            nextTrack.setPosition(0L);

        if (this.player.getPlayingTrack() != null)
            this.player.stopTrack();

        try {
            this.player.startTrack(nextTrack, false);
        } catch (IllegalStateException e) {
            this.player.startTrack(nextTrack.makeClone(), false);
        }

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (repeating) {
            if (track != null) {
                if (RobertifyAudioManager.getTracksRequestedByUsers().containsKey(track))
                    RobertifyAudioManager.removeRequester(track, RobertifyAudioManager.getRequester(track));
                try {
                    player.startTrack(track.makeClone(), false);
                } catch (UnsupportedOperationException e) {
                    track.setPosition(0);
                }
            } else {
                nextTrack();
                repeating = false;
            }
        } else if (endReason.mayStartNext) {
            pastQueue.push(track.makeClone());
            nextTrack();
        } else if (endReason.equals(AudioTrackEndReason.STOPPED)) {
            scheduleDisconnect(true);
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (!new TogglesConfig().getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) return;

        if (!new GuildConfig().announcementChannelIsSet(guild.getIdLong())) return;

        TextChannel announcementChannel = Robertify.api.getTextChannelById(new GuildConfig().getAnnouncementChannelID(this.guild.getIdLong()));
        try {
            announcementChannel.sendMessageEmbeds(
                            RobertifyEmbedUtils.embedMessage(
                                            guild,
                                            "`" + track.getInfo().title + "` by `" + track.getInfo().author + "` could not be played!\nSkipped to the next song. (If available)")
                                    .build()
                    )
                    .queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES));
        } catch (NullPointerException e) {
            new GuildConfig().setAnnouncementChannelID(guild.getIdLong(), -1L);
        } catch (InsufficientPermissionException ignored) {}

        nextTrack();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        logger.error("There was an exception with playing the track. Handling it.");
        exception.printStackTrace();
    }

    public void setSavedQueue(Guild guild, ConcurrentLinkedQueue<AudioTrack> queue) {
        ConcurrentLinkedQueue<AudioTrack> savedQueue = new ConcurrentLinkedQueue<>(queue);

        for (AudioTrack track : savedQueue)
            track.setPosition(0L);

        TrackScheduler.savedQueue.put(guild, savedQueue);
    }

    public void addToBeginningOfQueue(AudioTrack track) {
        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        newQueue.offer(track);
        newQueue.addAll(queue);
        queue = newQueue;
    }

    public void removeSavedQueue(Guild guild) {
        savedQueue.remove(guild);
    }

    public AudioTrack getLastPlayedTrack() {
        return pastQueue.pop();
    }

    public AudioTrack peekLastPlayedTrack() {
        return pastQueue.peek();
    }

    public void scheduleDisconnect(boolean announceMsg) {
        scheduleDisconnect(announceMsg, 5, TimeUnit.MINUTES);
    }

    public void scheduleDisconnect(boolean announceMsg, long delay, TimeUnit timeUnit) {
        if (new GuildConfig().get247(guild.getIdLong()))
            return;

        ScheduledFuture<?> schedule = executor.schedule(() -> {
            final var channel = guild.getSelfMember().getVoiceState().getChannel();

            if (!new GuildConfig().get247(guild.getIdLong())) {
                if (channel != null) {
                    guild.getAudioManager().closeAudioConnection();
                    disconnectExecutors.remove(guild.getIdLong());

                    final var guildConfig = new GuildConfig();

                    if (guildConfig.announcementChannelIsSet(guild.getIdLong()) && announceMsg)
                        Robertify.api.getTextChannelById(guildConfig.getAnnouncementChannelID(guild.getIdLong()))
                                .sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I have left " + channel.getAsMention() + " due to inactivity.").build())
                                .queue(msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES));
                }
            }
        }, delay, timeUnit);

        disconnectExecutors.putIfAbsent(guild.getIdLong(), schedule);
    }
}
