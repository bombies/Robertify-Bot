package main.audiohandlers.loaders

import dev.arbjerg.lavalink.client.player.Track
import dev.arbjerg.lavalink.client.player.TrackException
import dev.arbjerg.lavalink.protocol.v4.PlaylistInfo
import dev.minn.jda.ktx.util.SLF4J
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
) : AudioLoader(musicManager.guild) {

    companion object {
        private val logger by SLF4J
    }

    private val scheduler = musicManager.scheduler
    private val queueHandler = scheduler.queueHandler
    private val guild = musicManager.guild

    override fun onPlaylistLoad(playlist: List<Track>, playlistInfo: PlaylistInfo) {
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
        val mutableTracks = playlist.toMutableList()

        mutableTracks.forEach { track ->
            scheduler.unannouncedTracks.add(track.info.identifier)
            scheduler.addRequester(self.id, track.info.identifier)
        }
        
        scheduler.playTrack(mutableTracks.removeFirst())
        queueHandler.addAll(mutableTracks)

        if (queueHandler.queueRepeating) {
            queueHandler.queueRepeating = false
            queueHandler.clearPreviousTracks()
            queueHandler.clearSavedQueue()
        }

        RequestChannelConfig(guild).updateMessage()
    }

    override fun onSearchResultLoad(results: List<Track>) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override fun onTrackLoad(result: Track) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override fun onNoMatches() {
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

    override fun onException(exception: TrackException) {
        channel?.sendMessageEmbeds(
            RobertifyEmbedUtils.embedMessage(
                guild,
                AudioLoaderMessages.NO_SIMILAR_TRACKS
            ).build()
        )
            ?.queueThenDelete(time = 5, unit = TimeUnit.MINUTES) {
                musicManager.scheduler.scheduleDisconnect(announceMsg = true)
            }
        throw NullPointerException(
            "There were no similar tracks found!",
        )
    }
}