package main.audiohandlers

import dev.arbjerg.lavalink.client.*
import dev.arbjerg.lavalink.client.protocol.Track
import dev.arbjerg.lavalink.protocol.v4.TrackInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import main.audiohandlers.models.Requester
import main.audiohandlers.utils.artworkUrl
import main.commands.slashcommands.misc.PlaytimeCommand
import main.constants.Toggle
import main.main.Robertify
import main.utils.GeneralUtils.queueAfter
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TrackScheduler(private val guild: Guild) {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val audioManager = RobertifyAudioManager
    }

    private val link: Link
        get() = Robertify.lavalink.getLink(guild.idLong)
    private val requesters = ArrayList<Requester>()
    private var lastSentMsg: Message? = null
    val disconnectManager = GuildDisconnectManager(guild)
    val unannouncedTracks = emptyList<String>().toMutableList()
    val queueHandler = QueueHandler()
    val player: LavalinkPlayer? = link.getPlayer().block()
    var announcementChannel: GuildMessageChannel? = null

    private fun usePlayer(playerCallback: (LavalinkPlayer) -> Unit) {
        link.getPlayer().subscribe(playerCallback)
    }

    fun playTrack(track: Track) {
        this.link
            .createOrUpdatePlayer()
            .setTrack(track)
            .subscribe()
    }


    fun queue(track: Track) {
        usePlayer { player ->
            when {
                player.track == null -> playTrack(track)
                else -> queueHandler.add(track)
            }
        }
    }

    fun queue(tracks: Collection<Track>) {
        val mutableTracks = tracks.toMutableList()
        usePlayer { player ->
            if (player.track == null)
                playTrack(mutableTracks.removeFirst())
            queueHandler.addAll(mutableTracks)
        }
    }

    fun addToBeginningOfQueue(track: Track) = run {
        usePlayer { player ->
            when {
                player.track != null -> queueHandler.addToBeginning(track)
                else -> playTrack(track)
            }
        }
    }

    fun addToBeginningOfQueue(tracks: Collection<Track>) {
        val mutableTracks = tracks.toMutableList()

        usePlayer { player ->
            if (player.track == null)
                playTrack(mutableTracks.removeFirst())
            queueHandler.addToBeginning(mutableTracks)
        }
    }

    fun stop() {
        usePlayer { player ->
            queueHandler.clear()
            if (player.track != null)
                player.stopTrack()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun onTrackStart(event: TrackStartEvent) {
        val track = event.track

        logger.debug(
            "{} ({}) | Track started in {} ({}). Announcement channel: {}",
            link.state.name,
            link.guildId,
            guild.id,
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
        if (requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelId() == announcementChannel!!.idLong)
            return

        logger.debug("The channel is not the request channel")

        Robertify.shardManager.retrieveUserById(requester.id).queue { requesterUser ->
            val defaultBackgroundImage = ThemesConfig(
                guild
            ).getTheme().nowPlayingBanner

            val img: InputStream
            try {
                img = runBlocking {
                    NowPlayingImageBuilder(
                        artistName = track.info.author,
                        title = track.info.title,
                        albumImage = try {
                            track.artworkUrl ?: defaultBackgroundImage
                        } catch (e: MissingFieldException) {
                            defaultBackgroundImage
                        },
                        requesterName = requesterUser.name,
                        requesterAvatar = requesterUser.avatarUrl
                    ).build() ?: throw NullPointerException("The generated image was null!")
                }
            } catch (ex: Exception) {
                // Either building the image failed or the bot doesn't have enough
                // permission to send images in a certain channel
                if (ex is PermissionException || ex is ImageBuilderException || ex is NullPointerException) {
                    try {
                        sendNowPlayingEmbed(trackInfo, requesterMention)
                    } catch (ex: PermissionException) {
                        try {
                            sendNowPlayingString(track.info, requesterMention)
                        } catch (e: Exception) {
                            logger.warn("I was not able to send a now playing message at all in ${guild.name}")
                        }
                    } catch (ex: Exception) {
                        logger.error("Unexpected error", ex)
                    }
                } else {
                    logger.error("Unexpected error", ex)
                }
                return@queue
            }

            announcementChannel!!.sendFiles(
                FileUpload.fromData(
                    img,
                    AbstractImageBuilder.RANDOM_FILE_NAME
                )
            ).queue { imgMessage ->
                lastSentMsg?.delete()?.queueAfter(
                    3L, TimeUnit.SECONDS, null,
                    ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                )

                lastSentMsg = imgMessage
            }
        }
    }

    fun onTrackEnd(event: TrackEndEvent) {
        val reason = event.endReason

        logger.debug(
            "Track ended.\n" +
                    "May start next: ${reason.mayStartNext}\n" +
                    "End reason: ${reason.name}"
        )
        val trackToUse = queueHandler.lastPlayedTrackBuffer

        if (queueHandler.trackRepeating && reason.mayStartNext) {
            if (trackToUse == null) {
                queueHandler.trackRepeating = false
                nextTrack(null)
            } else playTrack(trackToUse.makeClone())
        } else if (reason.mayStartNext) {
            if (trackToUse != null)
                queueHandler.pushPastTrack(trackToUse)
            nextTrack(trackToUse)
        } else {
            if (queueHandler.isEmpty)
                RequestChannelConfig(guild).updateMessage()
        }
    }

    fun onTrackException(event: TrackExceptionEvent) {
        val exception = event.exception;
        val track = event.track

        val handleMessageCleanup: (msg: Message) -> Unit = { msg ->
            msg.delete().queueAfter(10.seconds)
            lastSentMsg?.delete()?.queueAfter(10.seconds)
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
        } else {
            logger.error("There was an exception with playing the track {}", exception.cause)
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    TrackSchedulerMessages.TRACK_COULD_NOT_BE_PLAYED,
                    Pair("{title}", track.info.title),
                    Pair("{author}", track.info.author),
                ).build()
            )?.queue(handleMessageCleanup)
        }
    }

    fun onTrackStuck(event: TrackStuckEvent) {
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
        val message = announcementChannel?.sendMessageEmbeds(embed)?.submit()
        message?.thenAcceptAsync { msg ->
            lastSentMsg = msg
            lastSentMsg?.delete()?.queueAfter(
                3L, TimeUnit.SECONDS, null,
                ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
            )
        }

        return message
    }

    private fun sendNowPlayingString(
        audioTrackInfo: TrackInfo,
        requesterMention: String
    ): CompletableFuture<Message>? {
        val embed = getNowPlayingEmbed(audioTrackInfo, requesterMention)
        val message = announcementChannel?.sendMessage(embed.description ?: "")
            ?.submit()
        message?.thenAcceptAsync { msg ->
            lastSentMsg = msg
            lastSentMsg?.delete()?.queueAfter(
                3L, TimeUnit.SECONDS, null,
                ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
            )
        }

        return message
    }

    fun nextTrack(lastTrack: Track?, skipped: Boolean = false, skippedAt: Long? = null) {
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
            playTrack(nextTrack)
        } else {
            if (lastTrack != null
                && AutoPlayConfig(guild).getStatus()
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
                player?.stopTrack()
                disconnectManager.scheduleDisconnect()
            }
        }
    }

    fun disconnect(announceMsg: Boolean = true) {
        val channel = guild.selfMember.voiceState?.channel ?: return
        if (GuildConfig(guild).getTwentyFourSevenMode())
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

    fun scheduleDisconnect(time: Duration = 5.minutes, announceMsg: Boolean = true) =
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