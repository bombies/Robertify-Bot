package main.audiohandlers

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import kotlinx.coroutines.runBlocking
import lavalink.client.io.Link
import lavalink.client.player.IPlayer
import lavalink.client.player.LavalinkPlayer
import lavalink.client.player.event.PlayerEventListenerAdapter
import main.audiohandlers.models.RequesterKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.source
import main.audiohandlers.utils.title
import main.constants.ToggleKt
import main.main.RobertifyKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.api.robertify.imagebuilders.AbstractImageBuilderKt
import main.utils.api.robertify.imagebuilders.ImageBuilderExceptionKt
import main.utils.api.robertify.imagebuilders.builders.NowPlayingImageBuilderKt
import main.utils.json.autoplay.AutoPlayConfigKt
import main.utils.json.guildconfig.GuildConfigKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit

class TrackSchedulerKt(private val guild: Guild, private val link: Link) : PlayerEventListenerAdapter() {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val audioManager = RobertifyAudioManagerKt
    }

    private val requesters = ArrayList<RequesterKt>()
    private var lastSentMsg: Message? = null
    val unannouncedTracks = emptyList<String>().toMutableList()
    val player: LavalinkPlayer
        get() = link.player
    val queueHandler = QueueHandlerKt()
    val disconnectManager = GuildDisconnectManagerKt(guild)
    var announcementChannel: GuildMessageChannel? = null

    fun queue(track: AudioTrack) = runBlocking {
        when {
            player.playingTrack != null -> queueHandler.add(track)
            else -> player.playTrack(track)
        }
    }

    fun addToBeginningOfQueue(track: AudioTrack) = run {
        when {
            player.playingTrack != null -> player.playTrack(track)
            else -> queueHandler.addToBeginning(track)
        }
    }

    fun addToBeginningOfQueue(tracks: Collection<AudioTrack>) {
        val mutableTracks = tracks.toMutableList()
        if (player.playingTrack == null) {
            player.playTrack(mutableTracks[0])
            mutableTracks.removeAt(0)
        }

        queueHandler.addToBeginning(mutableTracks)
    }

    fun stop() {
        queueHandler.clear()

        if (player.playingTrack != null)
            player.stopTrack()
    }

    override fun onTrackStart(player: IPlayer, track: AudioTrack) {
        logger.debug("${link.state.name} | AudioTrack started (${track.info.title}). Announcement channel: ${announcementChannel?.id ?: "undefined"}")
        val requestChannelConfig = RequestChannelConfigKt(guild)
        requestChannelConfig.updateMessage()

        disconnectManager.cancelDisconnect()
        queueHandler.lastPlayedTrackBuffer = track

        if (queueHandler.trackRepeating)
            return

        if (!TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES))
            return

        if (unannouncedTracks.contains(track.identifier)) {
            unannouncedTracks.remove(track.identifier)
        } else return

        if (announcementChannel == null)
            return

        val requester = findRequester(track.identifier) ?: return
        val requesterMention = getRequesterAsMention(track)
        if (requestChannelConfig.isChannelSet() && requestChannelConfig.channelId == announcementChannel!!.idLong)
            return

        RobertifyKt.shardManager.retrieveUserById(requester.id)
            .submit()
            .thenCompose { requesterObj ->
                val defaultBackgroundImage = ThemesConfigKt(
                    guild
                ).theme.nowPlayingBanner
                val img = NowPlayingImageBuilderKt(
                    artistName = track.author,
                    title = track.title,
                    albumImage = if (track is MirroringAudioTrack) track.artworkURL
                        ?: defaultBackgroundImage else defaultBackgroundImage,
                    requesterName = "${requesterObj.name}#${requesterObj.discriminator}",
                    requesterAvatar = requesterObj.avatarUrl
                ).build() ?: throw CompletionException(NullPointerException("The generated image was null!"))

                return@thenCompose announcementChannel!!.sendFiles(
                    FileUpload.fromData(
                        img,
                        AbstractImageBuilderKt.RANDOM_FILE_NAME
                    )
                )
                    .submit()
            }
            .thenApply { msg ->
                lastSentMsg?.delete()?.queueAfter(
                    3L, TimeUnit.SECONDS, null,
                    ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                )

                lastSentMsg = msg
                return@thenApply msg
            }
            .whenComplete imageCompletion@{ _, ex ->
                if (ex == null)
                    return@imageCompletion

                // Either building the image failed or the bot doesn't have enough
                // permission to send images in a certain channel
                if (ex is PermissionException || ex is ImageBuilderExceptionKt) {
                    sendNowPlayingEmbed(track.info, requesterMention)
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
            }
    }

    private fun getNowPlayingEmbed(trackInfo: AudioTrackInfo, requesterMention: String): MessageEmbed {
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        return RobertifyEmbedUtilsKt.embedMessage(
            guild, "${
                localeManager.getMessage(
                    NowPlayingMessages.NP_ANNOUNCEMENT_DESC,
                    Pair("{title}", trackInfo.title),
                    Pair("{author}", trackInfo.author)
                )
            }${
                if (TogglesConfigKt(guild).getToggle(ToggleKt.SHOW_REQUESTER))
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

    private fun sendNowPlayingEmbed(trackInfo: AudioTrackInfo, requesterMention: String): CompletableFuture<Message>? {
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
        audioTrackInfo: AudioTrackInfo,
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

    fun nextTrack(lastTrack: AudioTrack?, skipped: Boolean = false, skippedAt: Long? = null) {
        // TODO: Handle playtime updating

        if (queueHandler.isEmpty && queueHandler.queueRepeating)
            queueHandler.loadSavedQueue()

        val nextTrack = queueHandler.poll()

        if (nextTrack != null) {
            logger.debug("Retrieved ${nextTrack.title} and attempting to play")
            player.playTrack(nextTrack)

        } else {
            if (lastTrack != null
                && AutoPlayConfigKt(guild).status
                && lastTrack.source.equals("spotify", true)
            ) {
                val pastSpotifyTrackList = queueHandler.previousTracksContents
                    .filter { track -> track.source == "spotify" }
                    .map { track -> track.identifier }

                if (pastSpotifyTrackList.isEmpty()) {
                    disconnectManager.scheduleDisconnect()
                    return
                }

                val pastSpotifyTracks = pastSpotifyTrackList
                    .subList(0, pastSpotifyTrackList.size.coerceAtMost(5))
                    .toString()
                    .replace("[\\[\\]\\s]".toRegex(), "")

                audioManager.loadRecommendedTracks(
                    musicManager = RobertifyAudioManagerKt[guild],
                    channel = announcementChannel,
                    trackIds = pastSpotifyTracks
                )
            } else disconnectManager.scheduleDisconnect()
        }
    }

    override fun onTrackEnd(player: IPlayer, track: AudioTrack?, endReason: AudioTrackEndReason) {
        logger.debug(
            "AudioTrack ended.\n" +
                    "May start next: ${endReason.mayStartNext}\n" +
                    "End reason: ${endReason.name}"
        )
        val trackToUse = queueHandler.lastPlayedTrackBuffer

        if (queueHandler.trackRepeating) {
            if (trackToUse == null) {
                queueHandler.trackRepeating = false
                nextTrack(null)
            } else {
                player.playTrack(trackToUse.makeClone())
            }
        } else if (endReason.mayStartNext) {
            if (trackToUse != null)
                queueHandler.pushPastTrack(trackToUse)
            nextTrack(trackToUse)
        } else {
            RequestChannelConfigKt(guild).updateMessage()
        }
    }

    override fun onTrackException(player: IPlayer, track: AudioTrack?, exception: Exception) {
        val handleMessageCleanup: (msg: Message) -> Unit = { msg ->
            msg.delete().queueAfter(10, TimeUnit.SECONDS)
        }

        if (exception.message?.contains("matching track") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    TrackSchedulerMessages.COULD_NOT_FIND_SOURCE
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else if (exception.message?.contains("copyright") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild, TrackSchedulerMessages.COPYRIGHT_TRACK,
                    Pair("{title}", track?.title ?: "Unknown Track"),
                    Pair("{author}", track?.author ?: "Unknown Author")
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else if (exception.message?.contains("unavailable") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild, TrackSchedulerMessages.UNAVAILABLE_TRACK,
                    Pair("{title}", track?.title ?: "Unknown Track"),
                    Pair("{author}", track?.author ?: "Unknown Author")
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else if (exception.message?.contains("playlist type is unviewable") == true) {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    TrackSchedulerMessages.UNVIEWABLE_PLAYLIST
                ).build()
            )
                ?.queue(handleMessageCleanup)
        } else logger.error("There was an exception with playing the track.", exception)
    }

    override fun onTrackStuck(player: IPlayer, track: AudioTrack?, thresholdMs: Long) {
        if (!TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES))
            return

        try {
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    TrackSchedulerMessages.TRACK_COULD_NOT_BE_PLAYED,
                    Pair("{title}", track?.title ?: "Unknown Title"),
                    Pair("{author}", track?.author ?: "Unknown Author"),
                ).build()
            )?.queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
        } catch (_: InsufficientPermissionException) {
        }
        nextTrack(track)
    }

    fun disconnect(announceMsg: Boolean = true) {
        val channel = guild.selfMember.voiceState?.channel ?: return
        if (GuildConfigKt(guild).twentyFourSevenMode)
            return

        RobertifyAudioManagerKt
            .getMusicManager(guild)
            .leave()

        if (announceMsg && announcementChannel != null)
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
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

    fun scheduleDisconnect(time: Long = 5L, timeUnit: TimeUnit = TimeUnit.MINUTES, announceMsg: Boolean = true) =
        disconnectManager.scheduleDisconnect(time, timeUnit, announceMsg)

    fun removeScheduledDisconnect() =
        disconnectManager.cancelDisconnect()

    fun findRequester(trackId: String): RequesterKt? =
        requesters.find { it.trackId == trackId }

    fun addRequester(userId: String, trackId: String) =
        requesters.add(RequesterKt(userId, trackId))

    fun clearRequesters() = requesters.clear()

    fun getRequesterAsMention(track: AudioTrack): String {
        val requester = findRequester(track.identifier)
        return if (requester != null)
            "<@${requester.id}>"
        else
            LocaleManagerKt.getLocaleManager(guild)
                .getMessage(GeneralMessages.UNKNOWN_REQUESTER)
    }
}