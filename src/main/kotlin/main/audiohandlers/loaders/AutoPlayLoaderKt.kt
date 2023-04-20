package main.audiohandlers.loaders

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import dev.schlaubi.lavakord.Exception
import dev.schlaubi.lavakord.audio.player.Player
import dev.schlaubi.lavakord.rest.models.PartialTrack
import dev.schlaubi.lavakord.rest.models.TrackResponse
import main.audiohandlers.GuildMusicManagerKt
import main.audiohandlers.loaders.MainAudioLoaderKt.Companion.queueWithAutoDelete
import main.utils.RobertifyEmbedUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.concurrent.TimeUnit

class AutoPlayLoaderKt(
    private val musicManager: GuildMusicManagerKt,
    private val channel: GuildMessageChannel?,
    override val query: String
) : AudioLoader(musicManager.link) {

    private val scheduler = musicManager.scheduler
    private val queueHandler = scheduler.queueHandler
    private val guild = musicManager.guild

    override suspend fun trackLoaded(player: Player, track: PartialTrack) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override suspend fun playlistLoaded(
        player: Player,
        tracks: List<PartialTrack>,
        playlistInfo: TrackResponse.NullablePlaylistInfo
    ) {
        val self = guild.selfMember
        tracks.forEach { track ->
            scheduler.addRequester(self.id, track.info.identifier)
            scheduler.queue(track.toTrack())
        }

        if (queueHandler.isQueueRepeating) {
            queueHandler.isQueueRepeating = false
            queueHandler.clearPreviousTracks()
            queueHandler.clearSavedQueue()
        }

        RequestChannelConfigKt(guild).updateMessage()

        if (channel != null) {
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
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
    }

    override suspend fun searchLoaded(player: Player, tracks: List<PartialTrack>) {
        throw UnsupportedOperationException("This operation is not supported in the auto-play loader!")
    }

    override suspend fun noMatches() {
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

    override suspend fun loadFailed(exception: Exception?) {
        if (exception != null)
            throw Exception(exception.cause)
    }
}