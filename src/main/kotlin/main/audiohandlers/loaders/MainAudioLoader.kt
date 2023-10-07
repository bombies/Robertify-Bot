package main.audiohandlers.loaders

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import dev.arbjerg.lavalink.protocol.v4.Exception
import dev.arbjerg.lavalink.protocol.v4.Playlist
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.getDefaultScope
import kotlinx.coroutines.runBlocking
import main.audiohandlers.GuildMusicManager
import main.audiohandlers.models.Requester
import main.utils.RobertifyEmbedUtils
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.AudioLoaderMessages
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.requests.RestAction
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class MainAudioLoader(
    override val musicManager: GuildMusicManager,
    override val query: String,
    private val loadPlaylistShuffled: Boolean = false,
    private val addToBeginning: Boolean = false,
    private val announceMsg: Boolean = true,
    private val botMsg: Message? = null,
    _announcementChannel: GuildMessageChannel? = null,
    private val sender: User? = null,
) : AudioLoader(musicManager.guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val executorService = Executors.newSingleThreadScheduledExecutor()

        suspend fun RestAction<Message>.queueThenDelete(
            context: CoroutineContext = getDefaultScope().coroutineContext,
            time: Long = 10,
            unit: TimeUnit = TimeUnit.SECONDS,
            deletePredicate: (suspend (message: Message) -> Boolean)? = null,
            onSuccess: (suspend (message: Message) -> Unit)? = null
        ) {
            val message = this.await();
            if (deletePredicate == null || deletePredicate(message)) {
                executorService.schedule({
                    runBlocking(context) {
                        message.delete().await()
                        onSuccess?.invoke(message)
                    }
                }, time, unit)
            }
        }
    }

    private val guild = musicManager.guild
    private val scheduler = musicManager.scheduler
    private val queueHandler = scheduler.queueHandler
    private val announcementChannel: GuildMessageChannel? =
        _announcementChannel ?: botMsg?.channel?.asGuildMessageChannel()
    private val requestChannelConfig = RequestChannelConfig(guild)

    private suspend fun handleMessageUpdate(embed: MessageEmbed) {
        if (botMsg != null)
            botMsg.editMessageEmbeds(embed)
                .queueThenDelete(
                    deletePredicate = { msg -> requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelId() == msg.channel.idLong }
                )
        else {
            if (requestChannelConfig.isChannelSet())
                requestChannelConfig.getTextChannel()
                    ?.sendMessageEmbeds(embed)
                    ?.queueThenDelete()
            else logger.warn("${guild.name} | ${embed.description}")
        }
    }

    private suspend fun sendTrackLoadedMessage(track: Track) {
        val embed = RobertifyEmbedUtils.embedMessage(
            guild,
            AudioLoaderMessages.QUEUE_ADD,
            Pair("{title}", track.info.title),
            Pair("{author}", track.info.author)
        ).build()

        handleMessageUpdate(embed)
    }

    override suspend fun onPlaylistLoad(playlist: Playlist) {
        val mutableTracks = playlist.tracks.toMutableList()
        val embed = RobertifyEmbedUtils.embedMessage(
            guild, AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
            Pair("{numTracks}", mutableTracks.size.toString()),
            Pair("{playlist}", playlist.info.name)
        ).build()

        handleMessageUpdate(embed)

        if (announceMsg)
            mutableTracks.forEach { track -> scheduler.unannouncedTracks.add(track.info.identifier) }


        if (loadPlaylistShuffled)
            mutableTracks.shuffle()

        scheduler.announcementChannel = announcementChannel

        if (sender != null) {
            mutableTracks.forEach { track -> scheduler.addRequester(sender.id, track.info.identifier) }

            LogUtilsKt(guild).sendLog(
                LogType.QUEUE_ADD, AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
                Pair("{user}", sender.asMention),
                Pair("{numTracks}", mutableTracks.size.toString()),
                Pair("{playlist}", playlist.info.name)
            )
        }

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(mutableTracks)
        else
            scheduler.queue(mutableTracks)
    }

    override suspend fun onSearchResultLoad(results: List<Track>) {
        val firstResult = results.first()
        val trackInfo = firstResult.info

        if (announceMsg)
            scheduler.unannouncedTracks.add(trackInfo.identifier)

        if (sender != null)
            scheduler.addRequester(sender.id, trackInfo.identifier)

        scheduler.announcementChannel = announcementChannel

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(firstResult)
        else scheduler.queue(firstResult)

        sendTrackLoadedMessage(firstResult)
        val info = firstResult.info
        if (sender != null)
            LogUtilsKt(guild).sendLog(
                LogType.QUEUE_ADD, AudioLoaderMessages.QUEUE_ADD_LOG,
                Pair("{user}", sender.asMention),
                Pair("{title}", info.title),
                Pair("{author}", info.author)
            )
    }

    override suspend fun onTrackLoad(result: Track) {
        sendTrackLoadedMessage(result)

        val trackInfo = result.info

        if (announceMsg)
            scheduler.unannouncedTracks.add(trackInfo.identifier)

        val requester: Requester? = if (sender != null) {
            scheduler.addRequester(sender.id, trackInfo.identifier)
            Requester(sender.id, trackInfo.identifier)
        } else scheduler.findRequester(trackInfo.identifier)

        scheduler.announcementChannel = announcementChannel

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(result)
        else scheduler.queue(result)

        LogUtilsKt(guild).sendLog(
            LogType.QUEUE_ADD, AudioLoaderMessages.QUEUE_ADD_LOG,
            Pair("{user}", sender?.asMention ?: requester?.toMention() ?: "Unknown"),
            Pair("{title}", trackInfo.title),
            Pair("{author}", trackInfo.author)
        )

        if (queueHandler.queueRepeating)
            queueHandler.setSavedQueue(queueHandler.contents)

        requestChannelConfig.updateMessage()
    }

    override suspend fun onNoMatches() {
        val embed = if (query.length < 4096)
            RobertifyEmbedUtils.embedMessage(
                guild,
                AudioLoaderMessages.NO_TRACK_FOUND,
                Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
            )
                .build()
        else RobertifyEmbedUtils.embedMessage(guild, AudioLoaderMessages.NO_TRACK_FOUND_ALT)
            .build()

        handleMessageUpdate(embed)

        if (queueHandler.isEmpty && musicManager.player.playingTrack == null)
            scheduler.scheduleDisconnect(1.seconds, false)
    }

    override suspend fun onException(exception: Exception) {
        if (musicManager.player.playingTrack == null)
            musicManager.leave()

        if (exception.message?.contains("available") != true && exception.message?.contains("format") != true)
            logger.error("Could not load tracks in ${guild.name}!", exception)

        val embed = RobertifyEmbedUtils.embedMessage(
            guild,
            if (exception.message?.contains("available") == true || exception.message?.contains("format") == true)
                exception.message!!
            else LocaleManager[guild]
                .getMessage(AudioLoaderMessages.ERROR_LOADING_TRACK)
        ).build()

        handleMessageUpdate(embed)
    }
}