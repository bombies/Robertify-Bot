package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;
import lombok.Getter;
import lombok.Setter;
import main.audiohandlers.sources.deezer.DeezerSourceManager;
import main.audiohandlers.sources.spotify.SpotifySourceManager;
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
        this.playerManager = new DefaultAudioPlayerManager();

        this.playerManager.registerSourceManager(new SpotifySourceManager(playerManager));
        this.playerManager.registerSourceManager(new DeezerSourceManager(playerManager));
        this.playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        this.playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        this.playerManager.registerSourceManager(new HttpAudioSourceManager());
        this.playerManager.registerSourceManager(new BeamAudioSourceManager());
        this.playerManager.registerSourceManager(new LocalAudioSourceManager());
        this.playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        this.playerManager.registerSourceManager(new BandcampAudioSourceManager());
        this.playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        this.playerManager.registerSourceManager(new VimeoAudioSourceManager());
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
