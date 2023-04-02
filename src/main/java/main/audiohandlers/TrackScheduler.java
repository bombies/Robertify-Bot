package main.audiohandlers;

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
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
import main.utils.apis.robertify.imagebuilders.ImageBuilderException;
import main.utils.apis.robertify.imagebuilders.NowPlayingImageBuilder;
import main.utils.json.autoplay.AutoPlayConfig;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class TrackScheduler extends PlayerEventListenerAdapter {
    private final List<Requester> requesters = new ArrayList<>();

    @Getter
    private ConcurrentLinkedQueue<AudioTrack> savedQueue;
    private final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);

    private final Guild guild;
    private final Link audioPlayer;
    @Setter
    @Getter
    private GuildMessageChannel announcementChannel = null;
    private AudioTrack lastPlayedTrackBuffer;
    @Getter
    private final Stack<AudioTrack> pastQueue;
    @Getter
    private ConcurrentLinkedQueue<AudioTrack> queue;
    @Getter
    @Setter
    private boolean repeating = false;
    @Getter
    @Setter
    private boolean playlistRepeating = false;
    private Message lastSentMsg = null;
    private final DisconnectManager.GuildDisconnectManager disconnectManager;
    private final static RobertifyAudioManager audioManager = RobertifyAudioManager.getInstance();

    public TrackScheduler(Guild guild, Link audioPlayer) {
        this.guild = guild;
        this.audioPlayer = audioPlayer;
        this.queue = new ConcurrentLinkedQueue<>();
        this.pastQueue = new Stack<>();
        this.savedQueue = new ConcurrentLinkedQueue<>();
        this.disconnectManager = DisconnectManager.getInstance().getGuildDisconnector(guild);
    }

    public void queue(AudioTrack track) {
        if (getMusicPlayer().getPlayingTrack() != null) {
            queue.offer(track);
        } else {
            getMusicPlayer().playTrack(track);
        }
    }

    public void addToBeginningOfQueue(AudioTrack track) {
        if (getMusicPlayer().getPlayingTrack() == null) {
            getMusicPlayer().playTrack(track);
            return;
        }

        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        newQueue.offer(track);
        newQueue.addAll(queue);
        queue = newQueue;
    }

    public void addToBeginningOfQueue(List<AudioTrack> tracks) {
        if (getMusicPlayer().getPlayingTrack() == null) {
            getMusicPlayer().playTrack(tracks.get(0));
            tracks.remove(0);
        }

        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        tracks.forEach(newQueue::offer);
        newQueue.addAll(queue);
        queue = newQueue;
    }

    public void stop() {
        queue.clear();

        if (audioPlayer.getPlayer().getPlayingTrack() != null)
            audioPlayer.getPlayer().stopTrack();
    }

    @Override
    public void onTrackStart(IPlayer player, AudioTrack track) {
        disconnectManager.cancelDisconnect();
        lastPlayedTrackBuffer = track;

        if (repeating) return;

        if (!TogglesConfig.getConfig(guild).getToggle(Toggles.ANNOUNCE_MESSAGES)) return;

        if (RobertifyAudioManager.getUnannouncedTracks().contains(track.getIdentifier())) {
            RobertifyAudioManager.getUnannouncedTracks().remove(track.getIdentifier());
            return;
        }

        final var requester = findRequester(track.getIdentifier());
        final var requesterMention = RobertifyAudioManager.getRequesterAsMention(guild, track);

        if (announcementChannel != null) {
            final var dedicatedChannelConfig = new RequestChannelConfig(guild);
            if (dedicatedChannelConfig.isChannelSet())
                if (dedicatedChannelConfig.getChannelID() == announcementChannel.getIdLong())
                    return;

            final var trackInfo = track.getInfo();
            final var localeManager = LocaleManager.getLocaleManager(guild);
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(announcementChannel.getGuild(), localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_ANNOUNCEMENT_DESC, Pair.of("{title}", trackInfo.title), Pair.of("{author}", trackInfo.author))
                    + (TogglesConfig.getConfig(guild).getToggle(Toggles.SHOW_REQUESTER) ?
                    "\n\n" + localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER, Pair.of("{requester}", requesterMention))
                    :
                    ""
            ));

            try {
                final var img = new AtomicReference<File>();
                Robertify.getShardManager().retrieveUserById(requester.getId())
                        .submit()
                        .thenComposeAsync(requesterObj -> {
                            img.set(new NowPlayingImageBuilder()
                                    .setTitle(trackInfo.title)
                                    .setArtistName(trackInfo.author)
                                    .setAlbumImage(
                                            track instanceof MirroringAudioTrack mirroringAudioTrack ?
                                                    mirroringAudioTrack.getArtworkURL() :
                                                    new ThemesConfig(guild).getTheme().getNowPlayingBanner()
                                    )
                                    .setUser(requesterObj != null ? (requesterObj.getName() + "#" + requesterObj.getDiscriminator()) : requesterMention, requesterObj != null ? requesterObj.getAvatarUrl() : null)
                                    .build());
                            return announcementChannel.sendFiles(FileUpload.fromData(img.get())).submit();
                        })
                        .thenApplyAsync(msg -> {
                            img.get().delete();
                            if (lastSentMsg != null)
                                lastSentMsg.delete().queueAfter(3L, TimeUnit.SECONDS, null, new ErrorHandler()
                                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {
                                        }));
                            lastSentMsg = msg;
                            return msg;
                        })
                        .whenComplete((v, ex) -> {
                            if (ex == null)
                                return;

                            if (ex instanceof ImageBuilderException) {
                                logger.warn("I was unable to generate a now playing image in {}. Falling back to embed messages.", guild.getName());
                                announcementChannel.sendMessageEmbeds(eb.build()).queue(msg -> {
                                    if (lastSentMsg != null)
                                        lastSentMsg.delete().queueAfter(3L, TimeUnit.SECONDS, null, new ErrorHandler()
                                                .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {
                                                }));
                                    lastSentMsg = msg;
                                });
                            } else if (ex instanceof PermissionException) {
                                announcementChannel.sendMessageEmbeds(eb.build())
                                        .queue(embedMsg -> {
                                            if (lastSentMsg != null)
                                                lastSentMsg.delete().queueAfter(3L, TimeUnit.SECONDS, null, new ErrorHandler()
                                                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {
                                                        }));
                                            lastSentMsg = embedMsg;
                                        }, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e2 -> announcementChannel.sendMessage(eb.build().getDescription())
                                                .queue(nonEmbedMsg -> {
                                                    if (lastSentMsg != null)
                                                        lastSentMsg.delete().queueAfter(3L, TimeUnit.SECONDS, null, new ErrorHandler()
                                                                .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {
                                                                }));
                                                    lastSentMsg = nonEmbedMsg;
                                                })
                                        ));
                            }
                        });
            } catch (InsufficientPermissionException ignored) {
            }
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
                this.queue = new ConcurrentLinkedQueue<>(savedQueue);

        AudioTrack nextTrack = queue.poll();

        getMusicPlayer().stopTrack();

        if (nextTrack != null)
            getMusicPlayer().playTrack(nextTrack);
        else {
            if (lastTrack != null && new AutoPlayConfig(guild).getStatus() && lastTrack.getSourceManager().getSourceName().equalsIgnoreCase("spotify")) {
                final var pastSpotifyTackList = pastQueue.stream()
                        .filter(track -> track.getSourceManager().getSourceName().equals("spotify"))
                        .map(AudioTrack::getIdentifier)
                        .toList();
                final var pastSpotifyTracks = pastSpotifyTackList
                        .subList(0, Math.min(5, pastSpotifyTackList.size()))
                        .toString()
                        .replaceAll("[\\[\\]\\s]", "");

                audioManager.loadRecommendedTracks(
                        guild,
                        announcementChannel,
                        pastSpotifyTracks
                );
            } else disconnectManager.scheduleDisconnect(true);
        }

        final var dedicatedChannelConfig = new RequestChannelConfig(guild);
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
            } else {
                repeating = false;
                nextTrack(null);
            }
        } else if (endReason.mayStartNext) {
            pastQueue.push(trackToUse);
            nextTrack(trackToUse);
        }
    }

    @Override
    public void onTrackStuck(IPlayer player, AudioTrack track, long thresholdMs) {
        if (!TogglesConfig.getConfig(guild).getToggle(Toggles.ANNOUNCE_MESSAGES)) return;

        try {
            if (announcementChannel != null)
                announcementChannel.sendMessageEmbeds(
                                RobertifyEmbedUtils.embedMessage(
                                                guild,
                                                RobertifyLocaleMessage.TrackSchedulerMessages.TRACK_COULD_NOT_BE_PLAYED,
                                                Pair.of("{title}", track != null ? track.getInfo().title : "Unknown Title"),
                                                Pair.of("{author}", track != null ? track.getInfo().author : "Unknown Author")
                                        )
                                        .build()
                        )
                        .queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES));
        } catch (InsufficientPermissionException ignored) {
        }

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


    public void setSavedQueue(ConcurrentLinkedQueue<AudioTrack> queue) {
        this.savedQueue = new ConcurrentLinkedQueue<>(queue);
    }

    public void clearSavedQueue() {
        savedQueue.clear();
    }

    public void disconnect(boolean announceMsg) {
        final var channel = guild.getSelfMember().getVoiceState().getChannel();

//        Sussy line 🤔
//        if (RobertifyAudioManager.getInstance().getMusicManager(guild).getPlayer().getPlayingTrack() != null)
//            return;

        if (!new GuildConfig(guild).get247()) {
            if (channel != null) {
                RobertifyAudioManager.getInstance().getMusicManager(guild)
                        .leave();

                if (announceMsg && announcementChannel != null)
                    announcementChannel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.TrackSchedulerMessages.INACTIVITY_LEAVE, Pair.of("{channel}", channel.getAsMention())).build())
                            .queue(msg -> msg.delete()
                                    .queueAfter(
                                            2,
                                            TimeUnit.MINUTES,
                                            null,
                                            new ErrorHandler()
                                                    .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {
                                                    })
                                    )
                            );
            }
        }
    }

    public void scheduleDisconnect(boolean announceMsg) {
        disconnectManager.scheduleDisconnect(announceMsg);
    }

    public void scheduleDisconnect(boolean announceMsg, long time, TimeUnit timeUnit) {
        disconnectManager.scheduleDisconnect(announceMsg, time, timeUnit);
    }

    public void removeScheduledDisconnect() {
        DisconnectManager.getInstance()
                .destroyGuildDisconnector(guild);
    }

    public void removeSavedQueue(Guild guild) {
        savedQueue.remove(guild);
    }

    public IPlayer getMusicPlayer() {
        return audioPlayer.getPlayer();
    }

    public Requester findRequester(String trackId) {
        return this.requesters.stream()
                .filter(requester -> requester.getTrackId().equals(trackId))
                .findFirst()
                .orElse(null);
    }

    public void addRequester(String userId, String trackId) {
        this.requesters.add(new Requester(userId, trackId));
    }

    public void removeRequester(String userId) {
        final var newList = this.requesters.stream()
                .filter(requester -> !requester.getId().equals(userId))
                .toList();
        this.requesters.addAll(newList);
    }

    public void clearRequesters() {
        this.requesters.clear();
    }

    public static class Requester {
        @Getter
        private final String id;
        @Getter
        private final String trackId;

        private Requester(String id, String trackId) {
            this.id = id;
            this.trackId = trackId;
        }
    }
}
