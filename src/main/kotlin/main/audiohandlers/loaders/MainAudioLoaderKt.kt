package main.audiohandlers.loaders

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.runBlocking
import main.audiohandlers.GuildMusicManagerKt
import main.audiohandlers.models.RequesterKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class MainAudioLoaderKt(
    override val musicManager: GuildMusicManagerKt,
    override val query: String,
    private val loadPlaylistShuffled: Boolean = false,
    private val addToBeginning: Boolean = false,
    private val announceMsg: Boolean = true,
    private val botMsg: Message? = null,
    private val _announcementChannel: GuildMessageChannel? = null,
    private val sender: User? = null,
) : AudioLoader() {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        fun RestAction<Message>.queueWithAutoDelete(
            time: Long = 10,
            unit: TimeUnit = TimeUnit.SECONDS,
            deletePredicate: ((message: Message) -> Boolean)? = null,
            onSuccess: Consumer<Void>? = null
        ) {
            this.queue { msg ->
                if (deletePredicate == null || deletePredicate(msg))
                    msg.delete().queueAfter(time, unit, onSuccess, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
            }
        }
    }

    private val guild = musicManager.guild
    private val scheduler = musicManager.scheduler
    private val queueHandler = scheduler.queueHandler
    private val announcementChannel: GuildMessageChannel? =
        _announcementChannel ?: botMsg?.channel?.asGuildMessageChannel()
    private val requestChannelConfig = RequestChannelConfigKt(guild)

    override fun trackLoaded(track: AudioTrack) {
        sendTrackLoadedMessage(track)

        val trackInfo = track.info

        if (announceMsg)
            scheduler.unannouncedTracks.add(trackInfo.identifier)

        val requester: RequesterKt? = if (sender != null) {
            scheduler.addRequester(sender.id, trackInfo.identifier)
            RequesterKt(sender.id, trackInfo.identifier)
        } else scheduler.findRequester(trackInfo.identifier)

        scheduler.announcementChannel = announcementChannel

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(track)
        else scheduler.queue(track)

        val info = track.info
        LogUtilsKt(guild).sendLog(
            LogTypeKt.QUEUE_ADD, RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_ADD_LOG,
            Pair("{user}", sender?.asMention ?: requester.toString()),
            Pair("{title}", info.title),
            Pair("{author}", info.author)
        )

        if (queueHandler.isQueueRepeating)
            queueHandler.setSavedQueue(queueHandler.contents)

        requestChannelConfig.updateMessage()
    }

    private fun handleMessageUpdate(embed: MessageEmbed) {
        if (botMsg != null)
            botMsg.editMessageEmbeds(embed)
                .queueWithAutoDelete(
                    deletePredicate = { msg -> requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelID() == msg.channel.idLong }
                )
        else {
            if (requestChannelConfig.isChannelSet())
                requestChannelConfig.getTextChannel()
                    ?.sendMessageEmbeds(embed)
                    ?.queueWithAutoDelete()
            else logger.warn("${guild.name} | ${embed.description}")
        }
    }

    private fun sendTrackLoadedMessage(track: AudioTrack) {
        val embed = RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_ADD,
            Pair("{title}", track.info.title),
            Pair("{author}", track.info.author)
        ).build()

        handleMessageUpdate(embed)
    }

    override fun onPlaylistLoad(playlist: AudioPlaylist) {
        val mutableTracks = playlist.tracks.toMutableList()
        val embed = RobertifyEmbedUtilsKt.embedMessage(
            guild, RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
            Pair("{numTracks}", mutableTracks.size.toString()),
            Pair("{playlist}", playlist.name ?: "Unknown Playlist")
        ).build()

        handleMessageUpdate(embed)

        if (announceMsg)
            mutableTracks.forEach { track -> scheduler.unannouncedTracks.add(track.info.identifier) }


        if (loadPlaylistShuffled)
            mutableTracks.shuffle()

        scheduler.announcementChannel = announcementChannel

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(mutableTracks.map { it })

        if (sender != null)
            LogUtilsKt(guild).sendLog(
                LogTypeKt.QUEUE_ADD, RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
                Pair("{user}", sender.asMention),
                Pair("{numTracks}", mutableTracks.size.toString()),
                Pair("{playlist}", playlist.name ?: "Unknown Playlist")
            )

        mutableTracks.forEach { track ->
            if (sender != null)
                scheduler.addRequester(sender.id, track.info.identifier)

            if (!addToBeginning)
                scheduler.queue(track)
        }
    }

    override fun onSearchResultLoad(results: AudioPlaylist) {
        val firstResult = results.tracks.first()
        val trackInfo = firstResult.info

        if (announceMsg)
            scheduler.unannouncedTracks.add(trackInfo.identifier)

        if (sender != null)
            scheduler.addRequester(sender.id, trackInfo.identifier)

        scheduler.announcementChannel = announcementChannel

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(firstResult)
        else scheduler.queue(firstResult)

        val info = firstResult.info
        if (sender != null)
            LogUtilsKt(guild).sendLog(
                LogTypeKt.QUEUE_ADD, RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_ADD_LOG,
                Pair("{user}", sender.asMention),
                Pair("{title}", info.title),
                Pair("{author}", info.author)
            )
    }


    override fun noMatches() {
        val embed = if (query.length < 4096)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.AudioLoaderMessages.NO_TRACK_FOUND,
                Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
            )
                .build()
        else RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.AudioLoaderMessages.NO_TRACK_FOUND_ALT)
            .build()

        handleMessageUpdate(embed)

        if (queueHandler.isEmpty && musicManager.player.playingTrack == null)
            scheduler.scheduleDisconnect(1, TimeUnit.SECONDS, false)
    }

    override fun loadFailed(exception: FriendlyException?) {
        if (exception == null) {
            logger.warn("The loadFailed method was called while attempting to load tracks in ${guild.name} but the exception was null!")
            return
        }

        if (musicManager.player.playingTrack == null)
            musicManager.leave()

        if (exception.message?.contains("available") != true && exception.message?.contains("format") != true)
            logger.error("Could not load tracks in ${guild.name}!", exception)

        val embed = RobertifyEmbedUtilsKt.embedMessage(
            guild,
            if (exception.message?.contains("available") == true || exception.message?.contains("format") == true)
                exception.message!!
            else LocaleManagerKt.getLocaleManager(guild)
                .getMessage(RobertifyLocaleMessageKt.AudioLoaderMessages.ERROR_LOADING_TRACK)
        ).build()

        handleMessageUpdate(embed)
    }
}