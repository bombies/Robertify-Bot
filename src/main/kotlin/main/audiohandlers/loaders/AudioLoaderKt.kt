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
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.TimeUnit

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

    private val queueHandler = scheduler.queueHandler
    private val announcementChannel: GuildMessageChannel? = botMsg?.channel?.asGuildMessageChannel()
    private val requestChannelConfig = RequestChannelConfigKt(guild)

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

        if (botMsg != null) {
            botMsg.editMessageEmbeds(embed).queue { success ->
                success.editMessageComponents()
                    .queue { msg ->
                        if (requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelID() == msg.channel.idLong)
                            msg.delete().queueAfter(
                                10,
                                TimeUnit.SECONDS,
                                null,
                                ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                            )
                    }
            }
        } else if (requestChannelConfig.isChannelSet()) {
            requestChannelConfig.getTextChannel()
                ?.sendMessageEmbeds(embed)
                ?.queue { msg ->
                    msg.delete()
                        .queueAfter(10, TimeUnit.SECONDS, null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
                }
        }
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

                if (botMsg != null) {
                    botMsg.editMessageEmbeds(embed).queue { msg ->
                        if (requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelID() == msg.channel.idLong)
                            msg.delete().queueAfter(
                                10,
                                TimeUnit.SECONDS,
                                null,
                                ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                            )
                    }
                } else {
                    if (requestChannelConfig.isChannelSet())
                        requestChannelConfig.getTextChannel()!!.sendMessageEmbeds(embed)
                            .queue { msg ->
                                msg.delete().queueAfter(
                                    10,
                                    TimeUnit.SECONDS,
                                    null,
                                    ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)
                                )
                            }
                }

                if (!announceMsg)
                    tracks.forEach { track -> /* TODO: Unannounced track addition logic */ }

                if (loadPlaylistShuffled)
                    tracks.shuffle()

                scheduler.announcementChannel = announcementChannel

                if (addToBeginning)
                    scheduler.addToBeginningOfQueue(tracks)

                if (sender != null)
                    LogUtilsKt(guild).sendLog(LogTypeKt.QUEUE_ADD, RobertifyLocaleMessageKt.AudioLoaderMessages.QUEUE_PLAYLIST_ADD,
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
        TODO("Not yet implemented")
    }

    override fun loadFailed(exception: FriendlyException?) {
        TODO("Not yet implemented")
    }
}