package main.utils.resume

import main.audiohandlers.RobertifyAudioManagerKt
import net.dv8tion.jda.api.entities.Guild

class GuildResumeManagerKt(private val guild: Guild) {
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
        val playingTrack    = scheduler.audioPlayer.playingTrack

        if (playingTrack != null) {
            val requester = scheduler.findRequester(playingTrack.identifier)
            allTracks.add(ResumableTrackKt(playingTrack, requester))
        }

        allTracks.addAll(
            scheduler.queueHandler
                .contents
                .map { track -> ResumableTrackKt(track, scheduler.findRequester(track.identifier)) }
        )

        val resumeData = ResumeDataKt(channel, allTracks)
        resumeCache.data = resumeData
    }

}