package main.utils.resume

import com.fasterxml.jackson.core.JsonProcessingException
import main.audiohandlers.RobertifyAudioManager
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory

class GuildResumeManager(private val guild: Guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    private val resumeCache: GuildResumeCache = GuildResumeCache(guild.id)
    private val musicManager = RobertifyAudioManager[guild]
    private val scheduler = musicManager.scheduler
    val hasSave: Boolean = resumeCache.hasTracks

    fun saveTracks() {
        val selfVoiceState = guild.selfMember.voiceState
        if (selfVoiceState == null || !selfVoiceState.inAudioChannel())
            return

        val channel = selfVoiceState.channel!!.id
        val allTracks = mutableListOf<ResumableTrack>()
        val playingTrack = scheduler.player.playingTrack

        if (playingTrack != null) {
            val requester = scheduler.findRequester(playingTrack.identifier)
            allTracks.add(ResumableTrack(playingTrack, requester))
        }

        allTracks.addAll(
            scheduler.queueHandler
                .contents
                .map { track -> ResumableTrack(track, scheduler.findRequester(track.identifier)) }
        )

        val resumeData = ResumeData(channel, allTracks)
        resumeCache.data = resumeData
    }

    fun loadTracks() {
        if (!hasSave)
            return

        try {
            val loadedData = resumeCache.data!!
            RobertifyAudioManager.loadAndResume(musicManager, loadedData)
        } catch (e: JsonProcessingException) {
            logger.error("Couldn't load resume data for guild with id ${guild.id}", e)
        }
    }

}