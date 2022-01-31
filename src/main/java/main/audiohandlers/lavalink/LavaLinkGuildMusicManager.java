package main.audiohandlers.lavalink;

import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;
import lombok.Getter;
import lombok.Setter;
import main.audiohandlers.AbstractMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;

public class LavaLinkGuildMusicManager implements AbstractMusicManager {
    private final Guild guild;
    @Getter
    private final LavaLinkTrackScheduler scheduler;
    @Getter
    private final JdaLink link;
    @Getter
    private boolean isAwaitingDeath;
    @Setter @Getter
    private boolean forcePaused;

    public LavaLinkGuildMusicManager(Guild guild) {
        this.guild = guild;
        this.link = Robertify.getLavalink().getLink(guild);
        this.scheduler = new LavaLinkTrackScheduler(guild, link);
        link.getPlayer().addListener(this.scheduler);
    }

    public void leave() {
        getScheduler().queue.clear();

        if (getLink().getPlayer().getPlayingTrack() != null)
            getLink().getPlayer().stopTrack();

        getScheduler().repeating = false;
        getScheduler().playlistRepeating = false;
        getScheduler().getPastQueue().clear();

        if (getLink().getPlayer().isPaused())
            getLink().getPlayer().setPaused(false);

        if (guild == null) {
            link.destroy();
            return;
        }

        scheduler.stop();
        RobertifyAudioManager.getInstance().removeLavalinkMusicManager(guild);
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
