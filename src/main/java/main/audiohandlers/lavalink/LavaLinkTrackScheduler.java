package main.audiohandlers.lavalink;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.event.PlayerEventListenerAdapter;
import lombok.Getter;
import main.audiohandlers.AbstractTrackScheduler;
import main.audiohandlers.RobertifyAudioManager;
import main.constants.Toggles;
import main.main.Listener;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
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

public class LavaLinkTrackScheduler extends PlayerEventListenerAdapter implements AbstractTrackScheduler {
    private final static HashMap<Guild, ConcurrentLinkedQueue<AudioTrack>> savedQueue = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(LavaLinkTrackScheduler.class);


    private final Guild guild;
    private Link audioPlayer;
    @Getter
    private final Stack<AudioTrack> pastQueue;
    public ConcurrentLinkedQueue<AudioTrack> queue;
    public boolean repeating = false;
    public boolean playlistRepeating = false;
    private Message lastSentMsg = null;

    public LavaLinkTrackScheduler(Guild guild, Link audioPlayer) {
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
        if (repeating) return;

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
                                    })
                            ));
        } catch (NullPointerException e) {
            new GuildConfig().setAnnouncementChannelID(guild.getIdLong(), -1L);
        } catch (InsufficientPermissionException ignored) {}
    }

    public void nextTrack() {
        if (queue.isEmpty())
            if (playlistRepeating)
                this.queue = new ConcurrentLinkedQueue<>(savedQueue.get(guild));

        AudioTrack nextTrack = queue.poll();

        if (nextTrack != null)
            nextTrack.setPosition(0);

        if (getMusicPlayer().getPlayingTrack() != null)
            getMusicPlayer().stopTrack();

        try {
            getMusicPlayer().playTrack(nextTrack);
        } catch (IllegalStateException e) {
            getMusicPlayer().playTrack(nextTrack.makeClone());
        }

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);
    }

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
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
    public void onTrackStuck(IPlayer player, AudioTrack track, long thresholdMs) {
        Listener.logger.error("Track stuck. Attempting to replay the song.");
        handleTrackException(player, track);
    }

    @Override
    public void onTrackException(IPlayer player, AudioTrack track, Exception exception) {
        logger.error("There was an exception with playing the track. Handling it.");
        exception.printStackTrace();
        handleTrackException(player, track);
    }

    private void handleTrackException(IPlayer player, AudioTrack track) {
        // TODO handling exceptions
    }

    public void setSavedQueue(Guild guild, ConcurrentLinkedQueue<AudioTrack> queue) {
        ConcurrentLinkedQueue<AudioTrack> savedQueue = new ConcurrentLinkedQueue<>(queue);

        for (AudioTrack track : savedQueue)
            track.setPosition(0L);

        LavaLinkTrackScheduler.savedQueue.put(guild, savedQueue);
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

    public IPlayer getMusicPlayer() {
        return audioPlayer.getPlayer();
    }
}