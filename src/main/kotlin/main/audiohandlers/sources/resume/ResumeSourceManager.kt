package main.audiohandlers.sources.resume

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.topisenpai.lavasrc.mirror.DefaultMirroringAudioTrackResolver
import com.github.topisenpai.lavasrc.mirror.MirroringAudioSourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import main.utils.resume.ResumableTrack
import org.json.JSONArray
import org.slf4j.LoggerFactory
import java.io.DataInput

class ResumeSourceManager(playerManager: AudioPlayerManager) : MirroringAudioSourceManager(playerManager, DefaultMirroringAudioTrackResolver(emptyArray())) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        const val SEARCH_PREFIX = "rsearch:"
    }

    override fun getSourceName(): String = "resume"

    override fun loadItem(manager: AudioPlayerManager?, reference: AudioReference?): AudioItem? {
        if (manager == null || reference == null)
            return null

        try {
            if (!reference.identifier.startsWith(SEARCH_PREFIX))
                return null

            val obj     = JSONArray(reference.identifier.replace(SEARCH_PREFIX, ""))
            val queue   = emptyList<AudioTrack>().toMutableList()

            obj.spliterator()
                .forEachRemaining { o ->
                    try {
                        val resumableTrack = ResumableTrack.fromJSON(o.toString())
                        queue.add(resumableTrack.toAudioTrack(this))
                    } catch(e: JsonProcessingException) {
                        logger.warn("Could noy parse a track!: Data $o")
                    }
                }

            return when {
                queue.isEmpty() -> null
                queue.size > 1 -> BasicAudioPlaylist("Resumed Queue", queue, null, false)
                else -> queue[0]
            }
        } catch (e: Exception) {
            logger.error("Unexpected error attempting to resume tracks", e)
            return null
        }
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack =
        ResumeTrack(
            trackInfo,
            DataFormatTools.readNullableText(input),
            DataFormatTools.readNullableText(input),
            this
        )

    override fun shutdown() {}
}