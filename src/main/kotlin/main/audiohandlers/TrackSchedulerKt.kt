package main.audiohandlers

import com.github.topisenpai.lavasrc.applemusic.AppleMusicAudioTrack
import com.github.topisenpai.lavasrc.deezer.DeezerAudioTrack
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.github.topisenpai.lavasrc.spotify.SpotifyAudioTrack
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioTrack
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioTrack
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioTrack
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioTrack
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioTrack
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioTrack
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import dev.schlaubi.lavakord.audio.*
import dev.schlaubi.lavakord.audio.player.Track
import main.audiohandlers.models.RequesterKt
import main.audiohandlers.sources.resume.ResumeTrackKt
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
import main.utils.locale.messages.RobertifyLocaleMessageKt
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

class TrackSchedulerKt(private val guild: Guild, link: Link) {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val audioManager = RobertifyAudioManagerKt.ins

        fun Track.toAudioTrack(): AudioTrack {
            val trackInfo = AudioTrackInfo(title, author, length.inWholeMilliseconds, identifier, isStream, uri)
            return when (source) {
                "spotify" -> SpotifyAudioTrack(trackInfo, null, null, audioManager.spotifySourceManager)
                "deezer" -> DeezerAudioTrack(trackInfo, null, null, audioManager.deezerAudioSourceManager)
                "applemusic" -> AppleMusicAudioTrack(trackInfo, null, null, audioManager.appleMusicSourceManager)
                "soundcloud" -> SoundCloudAudioTrack(trackInfo, SoundCloudAudioSourceManager.createDefault())
                "youtube" -> YoutubeAudioTrack(trackInfo, YoutubeAudioSourceManager())
                "resume" -> ResumeTrackKt(trackInfo, null, null, audioManager.resumeSourceManager)
                "bandcamp" -> BandcampAudioTrack(trackInfo, BandcampAudioSourceManager())
                "vimeo" -> VimeoAudioTrack(trackInfo, VimeoAudioSourceManager())
                "twitch" -> TwitchStreamAudioTrack(trackInfo, TwitchStreamAudioSourceManager())
                "getyarn.io" -> GetyarnAudioTrack(trackInfo, GetyarnAudioSourceManager())
                "http" -> HttpAudioTrack(trackInfo, null, HttpAudioSourceManager())
                "local" -> LocalAudioTrack(trackInfo, null, LocalAudioSourceManager())
                else -> throw IllegalStateException("The source \"${source}\" hasn't been configured to provide a valid lavaplayer track!")
            }
        }
    }

    private val requesters = ArrayList<RequesterKt>()
    private var lastSentMsg: Message? = null
    val unannouncedTracks = emptyList<String>().toMutableList()
    val player = link.player
    val queueHandler = QueueHandlerKt()
    val disconnectManager = GuildDisconnectManagerKt(guild)
    var announcementChannel: GuildMessageChannel? = null

    init {
        trackStartEventHandler()
        trackEndEventHandler()
        trackExceptionEventHandler()
        trackStuckEventHandler()
    }

    suspend fun queue(track: Track) = run {
        when {
            player.playingTrack != null -> queueHandler.add(track)
            else -> player.playTrack(track)
        }
    }

    suspend fun addToBeginningOfQueue(track: Track) = run {
        when {
            player.playingTrack != null -> player.playTrack(track)
            else -> queueHandler.addToBeginning(track)
        }
    }

    suspend fun addToBeginningOfQueue(tracks: Collection<Track>) {
        val mutableTracks = tracks.toMutableList()
        if (player.playingTrack == null) {
            player.playTrack(mutableTracks[0])
            mutableTracks.removeAt(0)
        }

        queueHandler.addToBeginning(mutableTracks)
    }

    suspend fun stop() {
        queueHandler.clear()

        if (player.playingTrack != null)
            player.stopTrack()
    }

    private fun trackStartEventHandler() =
        player.on<Event, TrackStartEvent> {
            val track = getTrack()
            disconnectManager.cancelDisconnect()
            queueHandler.lastPlayedTrackBuffer = track

            if (queueHandler.isTrackRepeating)
                return@on

            if (!TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES))
                return@on

            if (unannouncedTracks.contains(track.identifier)) {
                unannouncedTracks.remove(track.identifier)
                return@on
            }

            if (announcementChannel == null)
                return@on

            val requester = findRequester(track.identifier) ?: return@on

            val requesterMention = getRequesterAsMention(track)

            val requestChannelConfig = RequestChannelConfigKt(guild)
            if (requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelID() == announcementChannel!!.idLong)
                return@on

            val lavaplayerTrack = track.toAudioTrack()
            RobertifyKt.shardManager.retrieveUserById(requester.id)
                .submit()
                .thenCompose { requesterObj ->
                    val img = NowPlayingImageBuilderKt(
                        artistName = track.author,
                        title = track.title,
                        albumImage = if (lavaplayerTrack is MirroringAudioTrack) lavaplayerTrack.artworkURL else ThemesConfigKt(
                            guild
                        ).theme.nowPlayingBanner,
                        requesterName = "${requesterObj.name}#${requesterObj.discriminator}",
                        requesterAvatar = requesterObj.avatarUrl
                    ).build() ?: throw CompletionException(NullPointerException("The generated image was null!"))

                    return@thenCompose announcementChannel!!.sendFiles(
                        FileUpload.fromData(
                            img,
                            AbstractImageBuilderKt.getRandomFileName()
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
                    if (ex.cause is PermissionException || ex.cause is ImageBuilderExceptionKt) {
                        sendNowPlayingEmbed(lavaplayerTrack.info, requesterMention)
                            ?.whenComplete embedCompletion@{ _, err ->
                                if (err == null)
                                    return@embedCompletion

                                // Probably doesn't have permission to send embeds
                                if (err is PermissionException)
                                    sendNowPlayingString(lavaplayerTrack.info, requesterMention)
                                        ?.whenComplete { _, stringErr ->
                                            if (stringErr != null)
                                                logger.warn("I was not able to send a now playing message at all in ${guild.name}")
                                        }
                            }
                    }
                }
        }

    private fun getNowPlayingEmbed(trackInfo: AudioTrackInfo, requesterMention: String): MessageEmbed {
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        return RobertifyEmbedUtilsKt.embedMessage(
            guild, "${
                localeManager.getMessage(
                    RobertifyLocaleMessageKt.NowPlayingMessages.NP_ANNOUNCEMENT_DESC,
                    Pair("{title}", trackInfo.title),
                    Pair("{author}", trackInfo.author)
                )
            }${
                if (TogglesConfigKt(guild).getToggle(ToggleKt.SHOW_REQUESTER))
                    "\n\n${
                        localeManager.getMessage(
                            RobertifyLocaleMessageKt.NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER,
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

    suspend fun nextTrack(lastTrack: Track?, skipped: Boolean = false, skippedAt: Long? = null) {
        // TODO: Handle playtime updating

        if (queueHandler.isEmpty && queueHandler.isQueueRepeating)
            queueHandler.loadSavedQueue()

        val nextTrack = queueHandler.poll()
        player.stopTrack()

        if (nextTrack != null)
            player.playTrack(nextTrack)
        else {
            if (lastTrack != null
                && AutoPlayConfigKt(guild).status
                && lastTrack.source.equals("spotify", true)
            ) {
                val pastSpotifyTrackList = queueHandler.previousTracksContents
                    .filter { track -> track.source == "spotify" }
                    .map { track -> track.identifier }
                val pastSpotifyTracks = pastSpotifyTrackList
                    .subList(0, pastSpotifyTrackList.size.coerceAtMost(5))
                    .toString()
                    .replace("[\\[\\]\\s]".toRegex(), "")

                audioManager.loadRecommendedTracks(
                    musicManager = RobertifyAudioManagerKt.ins.getMusicManager(guild),
                    channel = announcementChannel,
                    trackIds = pastSpotifyTracks
                )
            } else disconnectManager.scheduleDisconnect()
        }

        RequestChannelConfigKt(guild).updateMessage()
    }

    private fun trackEndEventHandler() =
        player.on<Event, TrackEndEvent> {
            val track = getTrack()
            val trackToUse = queueHandler.lastPlayedTrackBuffer

            if (queueHandler.isTrackRepeating) {
                if (trackToUse == null) {
                    queueHandler.isTrackRepeating = false
                    nextTrack(null)
                }
            } else if (reason.mayStartNext) {
                if (trackToUse != null)
                    queueHandler.pushPastTrack(trackToUse)
                nextTrack(trackToUse)
            }
        }

    private fun trackExceptionEventHandler() =
        player.on<Event, TrackExceptionEvent> {
            val track = getTrack()
            val handleMessageCleanup: (msg: Message) -> Unit = { msg ->
                msg.delete().queueAfter(10, TimeUnit.SECONDS)
            }

            if (exception.message.contains("matching track")) {
                announcementChannel?.sendMessageEmbeds(
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        RobertifyLocaleMessageKt.TrackSchedulerMessages.COULD_NOT_FIND_SOURCE
                    ).build()
                )
                    ?.queue(handleMessageCleanup)
            } else if (exception.message.contains("copyright")) {
                announcementChannel?.sendMessageEmbeds(
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild, RobertifyLocaleMessageKt.TrackSchedulerMessages.COPYRIGHT_TRACK,
                        Pair("{title}", track.title),
                        Pair("{author}", track.author)
                    ).build()
                )
                    ?.queue(handleMessageCleanup)
            } else if (exception.message.contains("unavailable")) {
                announcementChannel?.sendMessageEmbeds(
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild, RobertifyLocaleMessageKt.TrackSchedulerMessages.UNAVAILABLE_TRACK,
                        Pair("{title}", track.title),
                        Pair("{author}", track.author)
                    ).build()
                )
                    ?.queue(handleMessageCleanup)
            } else if (exception.message.contains("playlist type is unviewable")) {
                announcementChannel?.sendMessageEmbeds(
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        RobertifyLocaleMessageKt.TrackSchedulerMessages.UNVIEWABLE_PLAYLIST
                    ).build()
                )
                    ?.queue(handleMessageCleanup)
            } else logger.error("There was an exception with playing the track.", exception)
        }

    private fun trackStuckEventHandler() =
        player.on<Event, TrackStartEvent> {
            val track = getTrack()
            if (!TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES))
                return@on

            try {
                announcementChannel?.sendMessageEmbeds(
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        RobertifyLocaleMessageKt.TrackSchedulerMessages.TRACK_COULD_NOT_BE_PLAYED,
                        Pair("{title}", track.title),
                        Pair("{author}", track.author),
                    ).build()
                )?.queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
            } catch (_: InsufficientPermissionException) { }
            nextTrack(track)
        }

    suspend fun disconnect(announceMsg: Boolean = true) {
        val channel = guild.selfMember.voiceState?.channel ?: return
        if (GuildConfigKt(guild).twentyFourSevenMode)
            return

        RobertifyAudioManagerKt.ins
            .getMusicManager(guild)
            .leave()

        if (announceMsg && announcementChannel != null)
            announcementChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.TrackSchedulerMessages.INACTIVITY_LEAVE,
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

    fun getRequesterAsMention(track: Track): String {
        val requester = findRequester(track.identifier)
        return if (requester != null)
            "<@${requester.id}>"
        else
            LocaleManagerKt.getLocaleManager(guild)
                .getMessage(RobertifyLocaleMessageKt.GeneralMessages.UNKNOWN_REQUESTER)
    }
}