package main.audiohandlers.loaders;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.TrackScheduler;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoPlayLoader implements AudioLoadResultHandler {
    private final Logger logger = LoggerFactory.getLogger(AutoPlayLoader.class);

    private final GuildMusicManager musicManager;
    private final Guild guild;
    private final GuildMessageChannel channel;

    public AutoPlayLoader(GuildMusicManager musicManager, GuildMessageChannel channel) {
        this.musicManager = musicManager;
        this.guild = musicManager.getGuild();
        this.channel = channel;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        throw new UnsupportedOperationException("This operation is not supported in the auto-play loader");
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult())
            throw new UnsupportedOperationException("This operation is not supported in the auto-play loader");

        logger.info("Successfully loaded all recommended tracks for {}. ({} tracks)", guild.getName(), playlist.getTracks().size());
        final var scheduler = musicManager.getScheduler();

        final var tracksRequestedByUsers = RobertifyAudioManager.getTracksRequestedByUsers();
        tracksRequestedByUsers.putIfAbsent(guild.getIdLong(), new ArrayList<>());

        final var tracks = playlist.getTracks();
        for (final var track : tracks) {
            tracksRequestedByUsers.get(guild.getIdLong()).add(guild.getSelfMember().getId() + ":" + track.getIdentifier());
            scheduler.queue(track);
        }

        if (scheduler.isPlaylistRepeating()) {
            scheduler.setPlaylistRepeating(false);
            TrackScheduler.getPastQueue().clear();
            scheduler.clearSavedQueue(guild);
        }

        if (new RequestChannelConfig(guild).isChannelSet())
            new RequestChannelConfig(guild).updateMessage();

        if (channel != null) {
            final var localeManager = LocaleManager.getLocaleManager(guild);
            channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.AudioLoaderMessages.PLAYING_RECOMMENDED_TRACKS)
                    .setTitle(localeManager.getMessage(RobertifyLocaleMessage.AutoPlayMessages.AUTO_PLAY_EMBED_TITLE))
                    .setFooter(localeManager.getMessage(RobertifyLocaleMessage.AutoPlayMessages.AUTO_PLAY_EMBED_FOOTER))
                    .build()
            ).queue(msg -> msg.delete().queueAfter(5, TimeUnit.MINUTES));
        }
    }

    @Override
    public void noMatches() {
        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.AudioLoaderMessages.NO_SIMILAR_TRACKS).build())
                .queue(msg -> {
                    msg.delete().queueAfter(5, TimeUnit.MINUTES);
                    musicManager.getScheduler().scheduleDisconnect(true);
                });
        throw new FriendlyException("There were no similar tracks found!", FriendlyException.Severity.COMMON, new NullPointerException());
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        throw new FriendlyException("Unexpected error", FriendlyException.Severity.SUSPICIOUS, exception);
    }
}
