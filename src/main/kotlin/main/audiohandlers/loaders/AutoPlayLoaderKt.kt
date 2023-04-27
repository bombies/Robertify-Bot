package main.audiohandlers.loaders

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import main.audiohandlers.GuildMusicManagerKt
import main.audiohandlers.loaders.MainAudioLoaderKt.Companion.queueWithAutoDelete
import main.utils.RobertifyEmbedUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.concurrent.TimeUnit

class AutoPlayLoaderKt(
    override val musicManager: GuildMusicManagerKt,
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
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
            scheduler.announcementChannel = channel
            channel.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.AudioLoaderMessages.PLAYING_RECOMMENDED_TRACKS
                )
                    .setTitle(localeManager.getMessage(RobertifyLocaleMessageKt.AutoPlayMessages.AUTO_PLAY_EMBED_TITLE))
                    .setFooter(localeManager.getMessage(RobertifyLocaleMessageKt.AutoPlayMessages.AUTO_PLAY_EMBED_FOOTER))
                    .build()
            ).queueWithAutoDelete(5, TimeUnit.MINUTES)
        }

        val self = guild.selfMember
        playlist.tracks.forEach { track ->
            scheduler.addRequester(self.id, track.info.identifier)
            scheduler.queue(track)
        }

        if (queueHandler.isQueueRepeating) {
            queueHandler.isQueueRepeating = false
            queueHandler.clearPreviousTracks()
            queueHandler.clearSavedQueue()
        }

        RequestChannelConfigKt(guild).updateMessage()
    }

    override fun onSearchResultLoad(results: AudioPlaylist) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override fun noMatches() {
        channel?.sendMessageEmbeds(
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.AudioLoaderMessages.NO_SIMILAR_TRACKS
            ).build()
        )
            ?.queueWithAutoDelete(5, TimeUnit.MINUTES) {
                musicManager.scheduler.scheduleDisconnect(announceMsg = true)
            }
        throw FriendlyException("There were no similar tracks found!", FriendlyException.Severity.COMMON, NullPointerException())
    }

    override fun loadFailed(exception: FriendlyException?) {
        if (exception != null)
            throw Exception(exception.cause)
    }
}