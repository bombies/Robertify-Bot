package main.audiohandlers.loaders

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import main.audiohandlers.GuildMusicManager
import main.audiohandlers.loaders.MainAudioLoader.Companion.queueThenDelete
import main.utils.RobertifyEmbedUtils
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.AudioLoaderMessages
import main.utils.locale.messages.AutoPlayMessages
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.concurrent.TimeUnit

class AutoPlayLoader(
    override val musicManager: GuildMusicManager,
    private val channel: GuildMessageChannel?,
    override val query: String
) : AudioLoader() {

    private val scheduler = musicManager.scheduler
    private val queueHandler = scheduler.queueHandler
    private val guild = musicManager.guild

    override fun trackLoaded(track: AudioTrack) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override fun onPlaylistLoad(playlist: AudioPlaylist) {
        if (channel != null) {
            val localeManager = LocaleManager[guild]
            scheduler.announcementChannel = channel
            channel.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    AudioLoaderMessages.PLAYING_RECOMMENDED_TRACKS
                )
                    .setTitle(localeManager.getMessage(AutoPlayMessages.AUTO_PLAY_EMBED_TITLE))
                    .setFooter(localeManager.getMessage(AutoPlayMessages.AUTO_PLAY_EMBED_FOOTER))
                    .build()
            ).queueThenDelete(5, TimeUnit.MINUTES)
        }

        val self = guild.selfMember
        playlist.tracks.forEach { track ->
            scheduler.addRequester(self.id, track.info.identifier)
            scheduler.queue(track)
        }

        if (queueHandler.queueRepeating) {
            queueHandler.queueRepeating = false
            queueHandler.clearPreviousTracks()
            queueHandler.clearSavedQueue()
        }

        RequestChannelConfig(guild).updateMessage()
    }

    override fun onSearchResultLoad(results: AudioPlaylist) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override fun noMatches() {
        channel?.sendMessageEmbeds(
            RobertifyEmbedUtils.embedMessage(
                guild,
                AudioLoaderMessages.NO_SIMILAR_TRACKS
            ).build()
        )
            ?.queueThenDelete(5, TimeUnit.MINUTES) {
                musicManager.scheduler.scheduleDisconnect(announceMsg = true)
            }
        throw FriendlyException("There were no similar tracks found!", FriendlyException.Severity.COMMON, NullPointerException())
    }

    override fun loadFailed(exception: FriendlyException?) {
        if (exception != null)
            throw Exception(exception.cause)
    }
}