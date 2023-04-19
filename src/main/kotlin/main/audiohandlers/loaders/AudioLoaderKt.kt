package main.audiohandlers.loaders

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import main.audiohandlers.GuildMusicManagerKt
import main.audiohandlers.TrackSchedulerKt
import main.audiohandlers.models.RequesterKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
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

class AudioLoaderKt(
    private val guild: Guild,
    private val musicManager: GuildMusicManagerKt,
    private val scheduler: TrackSchedulerKt,
    private val trackUrl: String,
    private val loadPlaylistShuffled: Boolean = false,
    private val addToBeginning: Boolean = false,
    private val announceMsg: Boolean = true,
    private val botMsg: Message? = null,
    private val sender: User? = null,
) : AudioLoadResultHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    private val queueHandler = scheduler.queueHandler
    private val announcementChannel: GuildMessageChannel? = botMsg?.channel?.asGuildMessageChannel()
    private val requestChannelConfig = RequestChannelConfigKt(guild)

    private fun handleMessageUpdate(embed: MessageEmbed) {
        if (botMsg != null)
            botMsg.editMessageEmbeds(embed)
                .queueWithAutoDelete(
                    deletePredicate = { msg -> requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelID() == msg.channel.idLong }
                )
        else {
            requestChannelConfig.getTextChannel()
                ?.sendMessageEmbeds(embed)
                ?.queueWithAutoDelete()
        }
    }

    override fun trackLoaded(track: AudioTrack?) {
        if (track == null)
            return

        sendTrackLoadedMessage(track)

        if (!announceMsg) {
            // TODO: Handle unannounced track addition
        }

        var requester: RequesterKt? = if (sender != null) {
            scheduler.addRequester(sender.id, track.identifier)
            RequesterKt(sender.id, track.identifier)
        } else scheduler.findRequester(track.identifier)

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

    private fun sendTrackLoadedMessage(track: AudioTrack) {
        val embed = RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_ADD,
            Pair("{title}", track.info.title),
            Pair("{author}", track.info.author)
        ).build()

        handleMessageUpdate(embed)
    }

    override fun playlistLoaded(playlist: AudioPlaylist?) {
        if (playlist == null)
            return

        val tracks = playlist.tracks

        when {
            playlist.isSearchResult -> {
                val firstResult = tracks[0]

                if (!announceMsg) {
                    // TODO: Unannounced track addition handling
                }

                if (sender != null)
                    scheduler.addRequester(sender.id, firstResult.identifier)

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

            else -> {
                val embed = RobertifyEmbedUtilsKt.embedMessage(
                    guild, RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
                    Pair("{numTracks}", tracks.size.toString()),
                    Pair("{playlist}", playlist.name)
                ).build()

                handleMessageUpdate(embed)

                if (!announceMsg)
                    tracks.forEach { track -> /* TODO: Unannounced track addition logic */ }

                if (loadPlaylistShuffled)
                    tracks.shuffle()

                scheduler.announcementChannel = announcementChannel

                if (addToBeginning)
                    scheduler.addToBeginningOfQueue(tracks)

                if (sender != null)
                    LogUtilsKt(guild).sendLog(
                        LogTypeKt.QUEUE_ADD, RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
                        Pair("{user}", sender.asMention),
                        Pair("{numTracks}", tracks.size.toString()),
                        Pair("{playlist}", playlist.name)
                    )

                tracks.forEach { track ->
                    if (sender != null)
                        scheduler.addRequester(sender.id, track.identifier)

                    if (!addToBeginning)
                        scheduler.queue(track)
                }
            }
        }

        if (queueHandler.isQueueRepeating)
            queueHandler.setSavedQueue(queueHandler.contents)
        requestChannelConfig.updateMessage()
    }

    override fun noMatches() {
        val embed = if (trackUrl.length < 4096)
            RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.AudioLoaderMessages.NO_TRACK_FOUND)
                .build()
        else RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.AudioLoaderMessages.NO_TRACK_FOUND_ALT)
            .build()

        handleMessageUpdate(embed)

        if (queueHandler.isEmpty && musicManager.player.playingTrack == null)
            scheduler.scheduleDisconnect(1, TimeUnit.SECONDS, false)
    }

    override fun loadFailed(exception: FriendlyException?) {
        if (exception == null) {
            // Highly unlikely but due to Java interop Kotlin thinks the exception could be null
            logger.warn("The loadFailed method was called while attempting to load tracks in ${guild.name} but the exception was null!")
            return
        }

        if (musicManager.player.playingTrack == null)
            musicManager.leave()

        if (exception.message?.contains("available") == false && exception.message?.contains("format") == false)
            logger.error("Could not load tracks in ${guild.name}!", exception)

        val embed = RobertifyEmbedUtilsKt.embedMessage(guild,
            if (exception.message?.contains("available") == true || exception.message?.contains("format") == true)
                exception.message!!
            else LocaleManagerKt.getLocaleManager(guild).getMessage(RobertifyLocaleMessageKt.AudioLoaderMessages.ERROR_LOADING_TRACK)
        ).build()

        handleMessageUpdate(embed)
    }

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