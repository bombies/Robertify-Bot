package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;
import lombok.Getter;
import lombok.Setter;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;

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

    public GuildMusicManager(Guild guild) {
        this.guild = guild;
        this.link = Robertify.getLavalink().getLink(guild);
        this.scheduler = new TrackScheduler(guild, link);
        link.getPlayer().addListener(this.scheduler);
        this.playerManager = RobertifyAudioManager.getInstance().getPlayerManager();
    }

    public void leave() {
        getScheduler().getQueue().clear();

        getScheduler().setRepeating(false);
        getScheduler().setPlaylistRepeating(false);
        getScheduler().removeSavedQueue(guild);
        getScheduler().getPastQueue().clear();
        getPlayer().getFilters().clear().commit();
        RobertifyAudioManager.clearRequesters(guild);

        if (getPlayer().isPaused())
            getPlayer().setPaused(false);

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
