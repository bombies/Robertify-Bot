package main.utils.resume

import com.fasterxml.jackson.core.JsonProcessingException
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.TrackSchedulerKt.Companion.toAudioTrack
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory

class GuildResumeManagerKt(private val guild: Guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    private val resumeCache: GuildResumeCacheKt = GuildResumeCacheKt(guild.id)
    private val musicManager = RobertifyAudioManagerKt.ins.getMusicManager(guild)
    private val scheduler = musicManager.scheduler
    val hasSave: Boolean = resumeCache.hasTracks

    fun saveTracks() {
        val selfVoiceState = guild.selfMember.voiceState
        if (selfVoiceState == null || !selfVoiceState.inAudioChannel())
            return

        val channel         = selfVoiceState.channel!!.id
        val allTracks       = mutableListOf<ResumableTrackKt>()
        val playingTrack    = scheduler.player.playingTrack

        if (playingTrack != null) {
            val requester = scheduler.findRequester(playingTrack.identifier)
            allTracks.add(ResumableTrackKt(playingTrack.toAudioTrack(), requester))
        }

        allTracks.addAll(
            scheduler.queueHandler
                .contents
                .map { track -> ResumableTrackKt(track.toAudioTrack(), scheduler.findRequester(track.identifier)) }
        )

        val resumeData = ResumeDataKt(channel, allTracks)
        resumeCache.data = resumeData
    }

    suspend fun loadTracks() {
        if (!hasSave)
            return

        try {
            val loadedData = resumeCache.data!!
            RobertifyAudioManagerKt.ins.loadAndResume(musicManager, loadedData)
        } catch (e: JsonProcessingException) {
            logger.error("Couldn't load resume data for guild with id ${guild.id}", e)
        }
    }

}