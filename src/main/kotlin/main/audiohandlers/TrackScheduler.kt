package main.audiohandlers

import dev.arbjerg.lavalink.protocol.v4.Track
import dev.arbjerg.lavalink.protocol.v4.TrackInfo
import dev.minn.jda.ktx.coroutines.await
import dev.schlaubi.lavakord.audio.*
import dev.schlaubi.lavakord.audio.player.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import main.audiohandlers.models.Requester
import main.audiohandlers.utils.artworkUrl
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.misc.PlaytimeCommand
import main.constants.Toggle
import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageBuilderException
import main.utils.api.robertify.imagebuilders.builders.NowPlayingImageBuilder
import main.utils.database.influxdb.databases.tracks.TrackInfluxDatabase
import main.utils.internal.coroutines.RobertifyCoroutineScope
import main.utils.json.autoplay.AutoPlayConfig
import main.utils.json.guildconfig.GuildConfig
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.json.themes.ThemesConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.NowPlayingMessages
import main.utils.locale.messages.TrackSchedulerMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit
import kotlin.math.log
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TrackScheduler(private val guild: Guild, private val link: Link) {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val audioManager = RobertifyAudioManager
    }

    private val requesters = ArrayList<Requester>()
    private var lastSentMsg: Message? = null
    private val disconnectManager = GuildDisconnectManager(guild)

    val unannouncedTracks = emptyList<String>().toMutableList()
    val player: Player
        get() = link.player
    val queueHandler = QueueHandler()
    var announcementChannel: GuildMessageChannel? = null

    suspend fun queue(track: Track) {
        when {
            player.playingTrack != null -> queueHandler.add(track)
            else -> player.playTrack(track)
        }
    }

    suspend fun queue(tracks: Collection<Track>) {
        val mutableTracks = tracks.toMutableList()
        if (player.playingTrack == null)
            player.playTrack(mutableTracks.removeFirst())
        queueHandler.addAll(mutableTracks)
    }

    suspend fun addToBeginningOfQueue(track: Track) = run {
        when {
            player.playingTrack != null -> queueHandler.addToBeginning(track)
            else -> player.playTrack(track)
        }
    }

    suspend fun addToBeginningOfQueue(tracks: Collection<Track>) {
        val mutableTracks = tracks.toMutableList()
        if (player.playingTrack == null) {
            player.playTrack(mutableTracks.removeFirst())
        }

        queueHandler.addToBeginning(mutableTracks)
    }

    suspend fun stop() {
        queueHandler.clear()

        if (player.playingTrack != null)
            player.stopTrack()
    }

    init {
        player.events.onEach { event ->
            when (event) {
                is TrackStartEvent -> onTrackStart(event)
                is TrackStuckEvent -> onTrackStuck(event)
                is TrackExceptionEvent -> onTrackException(event)
                is TrackEndEvent -> onTrackEnd(event)
            }
        }
            .launchIn(player.coroutineScope)
    }

    private suspend fun onTrackStart(event: TrackStartEvent) {
        val track = event.track

        logger.debug(
            "{} | Track started ({}). Announcement channel: {}",
            link.state.name,
            track.info.title,
            announcementChannel?.id ?: "undefined"
        )
        val trackInfo = track.info
        val requester = findRequester(trackInfo.identifier)

        if (requester?.id != guild.selfMember.id)
            RobertifyCoroutineScope.launch {
                TrackInfluxDatabase.recordTrack(
                    title = trackInfo.title,
                    author = trackInfo.author,
                    guild = guild,
                    requester = requester
                )
            }

        val requestChannelConfig = RequestChannelConfig(guild)
        requestChannelConfig.updateMessage()

        disconnectManager.cancelDisconnect()
        queueHandler.lastPlayedTrackBuffer = track

        if (queueHandler.trackRepeating)
            return
        logger.debug("Track is not repeating")

        if (!TogglesConfig(guild).getToggle(Toggle.ANNOUNCE_MESSAGES))
            return

        logger.debug("Messages are being announced")

        if (unannouncedTracks.contains(trackInfo.identifier)) {
            unannouncedTracks.remove(trackInfo.identifier)
        } else return

        logger.debug("Teh track is unannounced")

        if (announcementChannel == null)
            return

        logger.debug("The announcement channel is not null")

        if (requester == null)
            return

        logger.debug("The requester is not null")

        val requesterMention = getRequesterAsMention(track)
        if (requestChannelConfig.isChannelSet() && requestChannelConfig.channelId == announcementChannel!!.idLong)
            return

        logger.debug("The channel is not the request channel")

        val requesterUser = Robertify.shardManager.retrieveUserById(requester.id).await()
        val defaultBackgroundImage = ThemesConfig(
            guild
        ).theme.nowPlayingBanner

        val img: InputStream
        try {
            img = NowPlayingImageBuilder(
                artistName = track.author,
                title = track.title,
                albumImage = track.artworkUrl ?: defaultBackgroundImage,
                requesterName = "${requesterUser.name}#${requesterUser.discriminator}",
                requesterAvatar = requesterUser.avatarUrl
            ).build() ?: throw NullPointerException("The generated image was null!")
        } catch (ex: Exception) {
            // Either building the image failed or the bot doesn't have enough
            // permission to send images in a certain channel
            if (ex is PermissionException || ex is ImageBuilderException) {
                sendNowPlayingEmbed(trackInfo, requesterMention)
                    ?.whenComplete embedCompletion@{ _, err ->
                        if (err == null)
                            return@embedCompletion

                        // Probably doesn't have permission to send embeds
                        if (err is PermissionException)
                            sendNowPlayingString(track.info, requesterMention)
                                ?.whenComplete { _, stringErr ->
                                    if (stringErr != null)
                                        logger.warn("I was not able to send a now playing message at all in ${guild.name}")
                                }
                        else logger.error("Unexpected error", ex)
                    }
            } else {
                logger.error("Unexpected error", ex)
            }
            return;
        }

        val imgMessage = announcementChannel!!.sendFiles(
            FileUpload.fromData(
                img,
                AbstractImageBuilder.RANDOM_FILE_NAME
            )
        ).await()

        lastSentMsg?.delete()?.queueAfter(
            3L, TimeUnit.SECONDS, null,
            ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
        )

        lastSentMsg = imgMessage
    }

    private suspend fun onTrackEnd(event: TrackEndEvent) {
        val reason = event.reason

        logger.debug(
            "Track ended.\n" +
                    "May start next: ${reason.mayStartNext}\n" +
                    "End reason: ${reason.name}"
        )
        val trackToUse = queueHandler.lastPlayedTrackBuffer

        if (queueHandler.trackRepeating) {
            if (trackToUse == null) {
                queueHandler.trackRepeating = false
                nextTrack(null)
            } else {
                player.playTrack(trackToUse.copy())
            }
        } else if (reason.mayStartNext) {
            if (trackToUse != null)
                queueHandler.pushPastTrack(trackToUse)
            nextTrack(trackToUse)
        } else {
            RequestChannelConfig(guild).updateMessage()
        }
    }

    private fun onTrackException(event: TrackExceptionEvent) {
        val exception = event.exception;
        val track = event.track

        val handleMessageCleanup: (msg: Message) -> Unit = { msg ->
            msg.delete().queueAfter(10, TimeUnit.SECONDS)
        }

        if (exception.message?.contains("matching track") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    TrackSchedulerMessages.COULD_NOT_FIND_SOURCE
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else if (exception.message?.contains("copyright") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild, TrackSchedulerMessages.COPYRIGHT_TRACK,
                    Pair("{title}", track.info.title),
                    Pair("{author}", track.info.author)
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else if (exception.message?.contains("unavailable") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild, TrackSchedulerMessages.UNAVAILABLE_TRACK,
                    Pair("{title}", track.info.title),
                    Pair("{author}", track.info.author)
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else if (exception.message?.contains("playlist type is unviewable") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    TrackSchedulerMessages.UNVIEWABLE_PLAYLIST
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else logger.error("There was an exception with playing the track {}", exception.cause)
    }

    private suspend fun onTrackStuck(event: TrackStuckEvent) {
        if (!TogglesConfig(guild).getToggle(Toggle.ANNOUNCE_MESSAGES))
            return

        val track = event.track

        try {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    TrackSchedulerMessages.TRACK_COULD_NOT_BE_PLAYED,
                    Pair("{title}", track.info.title),
                    Pair("{author}", track.info.author),
                ).build()
            )?.queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
        } catch (_: InsufficientPermissionException) {
        }
        nextTrack(track)
    }

    private fun getNowPlayingEmbed(trackInfo: TrackInfo, requesterMention: String): MessageEmbed {
        val localeManager = LocaleManager[guild]
        return RobertifyEmbedUtils.embedMessage(
            guild, "${
                localeManager.getMessage(
                    NowPlayingMessages.NP_ANNOUNCEMENT_DESC,
                    Pair("{title}", trackInfo.title),
                    Pair("{author}", trackInfo.author)
                )
            }${
                if (TogglesConfig(guild).getToggle(Toggle.SHOW_REQUESTER))
                    "\n\n${
                        localeManager.getMessage(
                            NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER,
                            Pair("{requester}", requesterMention)
                        )
                    }"
                else ""
            }"
        ).build()
    }

    private fun sendNowPlayingEmbed(trackInfo: TrackInfo, requesterMention: String): CompletableFuture<Message>? {
        val embed = getNowPlayingEmbed(trackInfo, requesterMention)
        return announcementChannel?.sendMessageEmbeds(embed)
            ?.submit()
            ?.thenApply { message ->
                lastSentMsg?.delete()?.queueAfter(
                    3L, TimeUnit.SECONDS, null,
                    ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                )
                lastSentMsg = message
                return@thenApply message
            }
    }

    private fun sendNowPlayingString(
        audioTrackInfo: TrackInfo,
        requesterMention: String
    ): CompletableFuture<Message>? {
        val embed = getNowPlayingEmbed(audioTrackInfo, requesterMention)
        return announcementChannel?.sendMessage(embed.description ?: "")
            ?.submit()
            ?.thenApply { message ->
                lastSentMsg?.delete()?.queueAfter(
                    3L, TimeUnit.SECONDS, null,
                    ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                )

                lastSentMsg = message
                return@thenApply message
            }
    }

    suspend fun nextTrack(lastTrack: Track?, skipped: Boolean = false, skippedAt: Long? = null) {
        val playtime = PlaytimeCommand.playtime
        if (lastTrack != null) {
            if (!skipped)
                playtime[guild.idLong] = if (playtime.containsKey(guild.idLong))
                    playtime[guild.idLong]!! + lastTrack.info.length
                else lastTrack.info.length
            else
                playtime[guild.idLong] = if (playtime.containsKey(guild.idLong))
                    playtime[guild.idLong]!! + (skippedAt ?: 0)
                else skippedAt ?: 0
        }

        if (queueHandler.isEmpty && queueHandler.queueRepeating)
            queueHandler.loadSavedQueue()

        val nextTrack = queueHandler.poll()

        if (nextTrack != null) {
            logger.debug("Retrieved {} and attempting to play", nextTrack.info.title)
            player.playTrack(nextTrack)

        } else {
            if (lastTrack != null
                && AutoPlayConfig(guild).status
                && lastTrack.info.sourceName.equals("spotify", true)
            ) {
                val pastSpotifyTrackList = queueHandler.previousTracksContents
                    .filter { track -> track.info.sourceName == "spotify" }
                    .map { track -> track.info.identifier }

                if (pastSpotifyTrackList.isEmpty()) {
                    disconnectManager.scheduleDisconnect()
                    return
                }

                val pastSpotifyTracks = pastSpotifyTrackList
                    .subList(0, pastSpotifyTrackList.size.coerceAtMost(5))
                    .toString()
                    .replace("[\\[\\]\\s]".toRegex(), "")

                audioManager.loadRecommendedTracks(
                    musicManager = RobertifyAudioManager[guild],
                    channel = announcementChannel,
                    trackIds = pastSpotifyTracks
                )
            } else {
                player.stopTrack()
                disconnectManager.scheduleDisconnect()
            }
        }
    }

    suspend fun disconnect(announceMsg: Boolean = true) {
        val channel = guild.selfMember.voiceState?.channel ?: return
        if (GuildConfig(guild).twentyFourSevenMode)
            return

        RobertifyAudioManager
            .getMusicManager(guild)
            .leave()

        if (announceMsg && announcementChannel != null)
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    TrackSchedulerMessages.INACTIVITY_LEAVE,
                    Pair("{channel}", channel.asMention)
                ).build()
            )?.queue { msg ->
                msg.delete().queueAfter(
                    2, TimeUnit.MINUTES, null,
                    ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                )
            }
    }

    suspend fun scheduleDisconnect(time: Duration = 5.minutes, announceMsg: Boolean = true) =
        disconnectManager.scheduleDisconnect(time, announceMsg)

    fun removeScheduledDisconnect() =
        disconnectManager.cancelDisconnect()

    fun findRequester(trackId: String): Requester? =
        requesters.find { it.trackId == trackId }

    fun addRequester(userId: String, trackId: String) =
        requesters.add(Requester(userId, trackId))

    fun addRequesters(requesters: Map<String, String>) =
        requesters.forEach { (userId, trackId) -> addRequester(userId, trackId) }

    fun clearRequesters() = requesters.clear()

    fun getRequesterAsMention(track: Track): String {
        val requester = findRequester(track.info.identifier)
        return if (requester != null)
            "<@${requester.id}>"
        else
            LocaleManager[guild]
                .getMessage(GeneralMessages.UNKNOWN_REQUESTER)
    }
}