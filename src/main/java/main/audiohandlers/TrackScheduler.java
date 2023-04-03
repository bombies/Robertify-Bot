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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class TrackScheduler extends PlayerEventListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);
    @Getter
    private final QueueHandler queueHandler;
    private final Guild guild;
    private final Link audioPlayer;
    @Setter
    @Getter
    private GuildMessageChannel announcementChannel = null;
    private Message lastSentMsg = null;
    @Getter
    private final GuildDisconnectManager disconnectManager;
    private final static RobertifyAudioManager audioManager = RobertifyAudioManager.getInstance();

    public TrackScheduler(Guild guild, Link audioPlayer) {
        this.guild = guild;
        this.audioPlayer = audioPlayer;
        this.queueHandler = new QueueHandler();
        this.disconnectManager = new GuildDisconnectManager(guild);
    }

    public void queue(AudioTrack track) {
        if (getMusicPlayer().getPlayingTrack() != null) {
            queueHandler.add(track);
        } else {
            getMusicPlayer().playTrack(track);
        }
    }

    public void addToBeginningOfQueue(AudioTrack track) {
        if (getMusicPlayer().getPlayingTrack() == null) {
            getMusicPlayer().playTrack(track);
            return;
        }

        queueHandler.addToBeginning(track);
    }

    public void addToBeginningOfQueue(List<AudioTrack> tracks) {
        if (getMusicPlayer().getPlayingTrack() == null) {
            getMusicPlayer().playTrack(tracks.get(0));
            tracks.remove(0);
        }

        queueHandler.addToBeginning(tracks);
    }

    public void stop() {
        queueHandler.clear();

        if (audioPlayer.getPlayer().getPlayingTrack() != null)
            audioPlayer.getPlayer().stopTrack();
    }

    @Override
    public void onTrackStart(IPlayer player, AudioTrack track) {
        disconnectManager.cancelDisconnect();
        queueHandler.setLastPlayedTrackBuffer(track);

        if (queueHandler.isTrackRepeating()) return;

        if (!TogglesConfig.getConfig(guild).getToggle(Toggles.ANNOUNCE_MESSAGES)) return;

        if (RobertifyAudioManager.getUnannouncedTracks().contains(track.getIdentifier())) {
            RobertifyAudioManager.getUnannouncedTracks().remove(track.getIdentifier());
            return;
        }

        final var requester = RobertifyAudioManager.getRequester(guild, track);

        if (announcementChannel != null) {
            final var dedicatedChannelConfig = new RequestChannelConfig(guild);
            if (dedicatedChannelConfig.isChannelSet())
                if (dedicatedChannelConfig.getChannelID() == announcementChannel.getIdLong())
                    return;

            final var trackInfo = track.getInfo();
            final var localeManager = LocaleManager.getLocaleManager(guild);
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(announcementChannel.getGuild(), localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_ANNOUNCEMENT_DESC, Pair.of("{title}", trackInfo.title), Pair.of("{author}", trackInfo.author))
                    + (TogglesConfig.getConfig(guild).getToggle(Toggles.SHOW_REQUESTER) ?
                    "\n\n" + localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER, Pair.of("{requester}", requester))
                    :
                    ""
            ));

            try {
                final var img = new AtomicReference<File>();
                Robertify.getShardManager().retrieveUserById(requester.replaceAll("[<@>]", ""))
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
                                    .setUser(requesterObj != null ? requesterObj.getName() + "#" + requesterObj.getDiscriminator() : requester, requesterObj != null ? requesterObj.getAvatarUrl() : null)
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

        if (queueHandler.isEmpty())
            if (queueHandler.isQueueRepeating())
                queueHandler.loadSavedQueue();

        AudioTrack nextTrack = queueHandler.poll();

        getMusicPlayer().stopTrack();

        if (nextTrack != null)
            getMusicPlayer().playTrack(nextTrack);
        else {
            if (lastTrack != null && new AutoPlayConfig(guild).getStatus() && lastTrack.getSourceManager().getSourceName().equalsIgnoreCase("spotify")) {
                audioManager.loadRecommendedTracks(
                        guild,
                        announcementChannel,
                        lastTrack
                );
            } else disconnectManager.scheduleDisconnect(true);
        }

        final var dedicatedChannelConfig = new RequestChannelConfig(guild);
        if (dedicatedChannelConfig.isChannelSet())
            dedicatedChannelConfig.updateMessage();
    }

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        final var trackToUse = queueHandler.getLastPlayedTrackBuffer();

        if (queueHandler.isTrackRepeating()) {
            if (trackToUse != null) {
                try {
                    AudioTrack clonedTrack = trackToUse.makeClone();
                    queueHandler.setLastPlayedTrackBuffer(clonedTrack);
                    player.playTrack(clonedTrack);
                } catch (UnsupportedOperationException e) {
                    player.seekTo(0);
                }
            } else {
                queueHandler.setTrackRepeating(false);
                nextTrack(null);
            }
        } else if (endReason.mayStartNext) {
            queueHandler.pushPastTrack(trackToUse);
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
        disconnectManager.cancelDisconnect();
    }

    public IPlayer getMusicPlayer() {
        return audioPlayer.getPlayer();
    }
}
