package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import main.main.Robertify;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.toggles.Toggles;
import main.utils.json.legacy.togglesconfig.LegacyTogglesConfig;
import main.main.Listener;
import main.utils.database.sqlite3.BotDB;
import main.utils.json.legacy.dedicatedchannel.LegacyDedicatedChannelConfig;
import main.utils.json.toggles.TogglesConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class TrackScheduler extends AudioEventAdapter {

    private final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);

    public final AudioPlayer player;
    private final static HashMap<Guild, ConcurrentLinkedQueue<AudioTrack>> savedQueue = new HashMap<>();
    private final static HashMap<Guild, ConcurrentLinkedQueue<AudioTrackInfo>> savedInfoQueue = new HashMap<>();
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
        if (!repeating) {
            if (RobertifyAudioManager.getUnannouncedTracks().contains(track)) {
                RobertifyAudioManager.getUnannouncedTracks().remove(track);
                return;
            }

            final var requester = RobertifyAudioManager.getRequester(track);
            TextChannel announcementChannel = Robertify.api.getTextChannelById(new GuildConfig().getAnnouncementChannelID(this.guild.getIdLong()));
            EmbedBuilder eb = EmbedUtils.embedMessage("Now Playing: `" + track.getInfo().title + "` by `"+track.getInfo().author+"`"
                    + ((new TogglesConfig().getToggle(guild, Toggles.SHOW_REQUESTER) && requester != null) ?
                    " [" + requester.getAsMention() + "]"
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
                        });
            } catch (InsufficientPermissionException ignored) {
            }
        }
    }

    public void nextTrack() {
        if (this.queue.isEmpty())
            if (playlistRepeating)
                this.queue = new ConcurrentLinkedQueue<>(savedQueue.get(this.guild));

        AudioTrack nextTrack = this.queue.poll();

        if (nextTrack != null)
            nextTrack.setPosition(0L);

        try {
            this.player.startTrack(nextTrack, false);
        } catch (IllegalStateException e) {
            this.player.startTrack(nextTrack.makeClone(), false);
        }

        if (new LegacyDedicatedChannelConfig().isChannelSet(guild.getId()))
            new LegacyDedicatedChannelConfig().updateMessage(guild);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (repeating) {
            if (track != null) {
                if (RobertifyAudioManager.getTracksRequestedByUsers().containsKey(track))
                    RobertifyAudioManager.removeRequester(track, RobertifyAudioManager.getRequester(track));
                try {
                    player.playTrack(track.makeClone());
                } catch (UnsupportedOperationException e) {
                    track.setPosition(0);
                }
            } else nextTrack();
        } else if (endReason.mayStartNext) {
            pastQueue.push(track.makeClone());
            nextTrack();
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        Listener.logger.error("Track stuck. Attempting to replay the song.");
        handleTrackException(player, track);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        logger.error("There was an exception with playing the track. Handling it.");
        exception.printStackTrace();
        handleTrackException(player, track);
    }

    private void handleTrackException(AudioPlayer player, AudioTrack track) {
        // TODO handling exceptions
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
}
