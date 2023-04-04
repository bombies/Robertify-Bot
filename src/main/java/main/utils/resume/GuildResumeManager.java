package main.utils.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.TrackScheduler;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@Slf4j
public class GuildResumeManager {
    private final Guild guild;
    private final GuildResumeCache resumeCache;
    private final GuildMusicManager musicManager;
    private final TrackScheduler scheduler;

    public GuildResumeManager(@NotNull Guild guild) {
        this.guild = guild;
        this.musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        this.scheduler = musicManager.getScheduler();
        this.resumeCache = new GuildResumeCache(guild.getId());
    }

    public void saveTracks() {
        final var selfVoiceState = guild.getSelfMember().getVoiceState();
        if (selfVoiceState == null || !selfVoiceState.inAudioChannel())
            return;

        final var channel = selfVoiceState.getChannel().getId();
        final var allTracks = new ArrayList<ResumableTrack>();
        final var playingTrack = scheduler.getMusicPlayer().getPlayingTrack();

        if (playingTrack != null) {
            final var requester = scheduler.findRequester(playingTrack.getIdentifier());
            allTracks.add(new ResumableTrack(playingTrack, requester));
        }

        allTracks.addAll(
                scheduler.getQueueHandler()
                        .contents()
                        .stream()
                        .map(track -> new ResumableTrack(track, scheduler.findRequester(track.getIdentifier())))
                        .toList()
        );

        log.info("Saving tracks for {}. ({} track(s))", guild.getName(), allTracks.size());
        resumeCache.setTracks(new ResumeData(channel, scheduler.getAnnouncementChannel().getId(),  allTracks));
    }

    public boolean hasSave() {
        return resumeCache.hasTracks();
    }

    public void loadTracks() {
        if (!hasSave())
            return;
        try {
            final var loadedData = resumeCache.loadData();
            log.info("Restarting {} tracks in {}", loadedData.getTracks().size(), guild.getName());
            RobertifyAudioManager.getInstance().loadAndResume(musicManager, loadedData);
        } catch (JsonProcessingException e) {
            log.error("Could not load resume data for guild with id {}", guild.getId(), e);
        }
    }
}
