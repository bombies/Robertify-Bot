package main.audiohandlers;

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
    @Setter @Getter
    private boolean forcePaused;

    public GuildMusicManager(Guild guild) {
        this.guild = guild;
        this.link = Robertify.getLavalink().getLink(guild);
        this.scheduler = new TrackScheduler(guild, link);
        link.getPlayer().addListener(this.scheduler);
    }

    public void leave() {
        getScheduler().queue.clear();

        if (getLink().getPlayer().getPlayingTrack() != null)
            getLink().getPlayer().stopTrack();

        getScheduler().repeating = false;
        getScheduler().playlistRepeating = false;
        getScheduler().clearSavedQueue(guild);
        getScheduler().getPastQueue().clear();
        getPlayer().getFilters().clear().commit();
        RobertifyAudioManager.clearRequesters(guild);

        if (getLink().getPlayer().isPaused())
            getLink().getPlayer().setPaused(false);

        scheduler.stop();
        RobertifyAudioManager.getInstance().removeMusicManager(guild);
    }

    public void destroy() {
        getLink().getPlayer().removeListener(scheduler);
        getLink().resetPlayer();

        if (!getLink().getState().equals(Link.State.DESTROYED))
                getLink().destroy();
    }

    public LavalinkPlayer getPlayer() {
        return link.getPlayer();
    }
}
