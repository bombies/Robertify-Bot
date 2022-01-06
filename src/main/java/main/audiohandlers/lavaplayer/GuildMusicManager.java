package main.audiohandlers.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lombok.Getter;
import lombok.Setter;
import main.audiohandlers.AbstractMusicManager;
import main.commands.commands.audio.LofiCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.entities.Guild;

public class GuildMusicManager implements AbstractMusicManager {
    @Getter
    private final AudioPlayer player;
    @Getter
    private final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;
    @Getter
    private final Guild guild;
    @Setter @Getter
    private boolean forcePaused;

    public GuildMusicManager(AudioPlayerManager manager, Guild guild) {
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.player, guild);
        this.player.addListener(this.scheduler);
        this.guild = guild;
        this.sendHandler = new AudioPlayerSendHandler(this.player);
    }

    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }

    public void leave() {
        scheduler.queue.clear();

        if (scheduler.player.getPlayingTrack() != null)
            scheduler.player.stopTrack();

        scheduler.repeating = false;
        scheduler.playlistRepeating = false;
        scheduler.getPastQueue().clear();

        if (scheduler.player.isPaused())
            scheduler.player.setPaused(false);

        guild.getAudioManager().closeAudioConnection();

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        LofiCommand.getLofiEnabledGuilds().remove(guild.getIdLong());
    }
}
