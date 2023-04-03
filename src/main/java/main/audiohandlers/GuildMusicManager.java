package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;
import lombok.Getter;
import lombok.Setter;
import main.commands.prefixcommands.audio.SkipCommand;
import main.main.Robertify;
import main.utils.json.requestchannel.RequestChannelConfig;
import net.dv8tion.jda.api.entities.Guild;

import java.util.concurrent.CompletableFuture;

public class GuildMusicManager {
    @Getter
    private final Guild guild;
    @Getter
    private final TrackScheduler scheduler;
    @Getter
    private final JdaLink link;
    @Getter
    private final AudioPlayerManager playerManager;
    @Setter @Getter
    private boolean forcePaused;

    GuildMusicManager(Guild guild) {
        this.guild = guild;
        this.link = Robertify.getLavalink().getLink(guild);
        this.scheduler = new TrackScheduler(guild, link);
        link.getPlayer().addListener(this.scheduler);
        this.playerManager = RobertifyAudioManager.getInstance().getPlayerManager();
    }

    public void clear() {
        final var queueHandler = getScheduler().getQueueHandler();

        queueHandler.clear();
        queueHandler.clearSavedQueue();
        queueHandler.clearPreviousTracks();

        queueHandler.setTrackRepeating(false);
        queueHandler.setQueueRepeating(false);
        getPlayer().getFilters().clear().commit();

        if (getPlayer().getPlayingTrack() != null)
            getPlayer().stopTrack();

        if (getPlayer().isPaused())
            getPlayer().setPaused(false);

        RobertifyAudioManager.clearRequesters(guild);
        SkipCommand.clearVoteSkipInfo(guild);

        final var dedicatedChannelConfig = new RequestChannelConfig(guild);
        dedicatedChannelConfig.updateMessage();
    }

    public void leave() {
        clear();
        scheduler.stop();
        RobertifyAudioManager.getInstance().removeMusicManager(guild);
    }

    public void destroy() {
        getPlayer().removeListener(scheduler);

        if (!getLink().getState().equals(Link.State.DESTROYED))
                getLink().destroy();
    }

    public LavalinkPlayer getPlayer() {
        return link.getPlayer();
    }
}
