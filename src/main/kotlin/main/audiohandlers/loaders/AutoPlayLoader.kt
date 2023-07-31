package main.audiohandlers.loaders

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import dev.arbjerg.lavalink.protocol.v4.Playlist
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.runBlocking
import main.audiohandlers.GuildMusicManager
import main.audiohandlers.loaders.MainAudioLoader.Companion.queueThenDelete
import main.audiohandlers.utils.identifier
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
) : AudioLoader(musicManager.guild) {

    companion object {
        private val logger by SLF4J
    }

    private val scheduler = musicManager.scheduler
    private val queueHandler = scheduler.queueHandler
    private val guild = musicManager.guild

    override suspend fun onPlaylistLoad(playlist: Playlist) {
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
            ).queueThenDelete(time = 5, unit = TimeUnit.MINUTES)
        }

        val self = guild.selfMember
        val mutableTracks = playlist.tracks.toMutableList()

        mutableTracks.forEach { track ->
            scheduler.unannouncedTracks.add(track.identifier)
            scheduler.addRequester(self.id, track.info.identifier)
        }

        scheduler.player.playTrack(mutableTracks.removeFirst())
        queueHandler.addAll(mutableTracks)

        if (queueHandler.queueRepeating) {
            queueHandler.queueRepeating = false
            queueHandler.clearPreviousTracks()
            queueHandler.clearSavedQueue()
        }

        RequestChannelConfig(guild).updateMessage()
    }

    override suspend fun onSearchResultLoad(results: List<Track>) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override suspend fun onTrackLoad(result: Track) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override suspend fun onNoMatches() {
        channel?.sendMessageEmbeds(
            RobertifyEmbedUtils.embedMessage(
                guild,
                AudioLoaderMessages.NO_SIMILAR_TRACKS
            ).build()
        )
            ?.queueThenDelete(time = 5, unit = TimeUnit.MINUTES) {
                musicManager.scheduler.scheduleDisconnect(announceMsg = true)
            }
    }

    override suspend fun onException(exception: dev.arbjerg.lavalink.protocol.v4.Exception) {
        channel?.sendMessageEmbeds(
            RobertifyEmbedUtils.embedMessage(
                guild,
                AudioLoaderMessages.NO_SIMILAR_TRACKS
            ).build()
        )
            ?.queueThenDelete(time = 5, unit = TimeUnit.MINUTES) {
                musicManager.scheduler.scheduleDisconnect(announceMsg = true)
            }
        throw FriendlyException(
            "There were no similar tracks found!",
            FriendlyException.Severity.COMMON,
            NullPointerException()
        )
    }
}