package main.audiohandlers.lavalink;

import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;
import lombok.Getter;
import lombok.Setter;
import main.audiohandlers.AbstractMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;

public class GuildMusicManager implements AbstractMusicManager {
    private final Guild guild;
    @Getter
    private final TrackScheduler scheduler;
    @Getter
    private final JdaLink link;
    @Getter
    private boolean isAwaitingDeath;
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
        RobertifyAudioManager.clearRequesters(guild);

        if (getLink().getPlayer().isPaused())
            getLink().getPlayer().setPaused(false);

        if (guild == null) {
            link.destroy();
            return;
        }

        scheduler.stop();
        RobertifyAudioManager.getInstance().removeMusicManager(guild);
    }

    public void destroy() {
        getLink().getPlayer().removeListener(scheduler);
        getLink().resetPlayer();
        getLink().destroy();
    }

    public LavalinkPlayer getPlayer() {
        return link.getPlayer();
    }

    @Override
    public Guild getGuild() {
        return guild;
    }
}
