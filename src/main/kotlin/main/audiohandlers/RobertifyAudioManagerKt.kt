package main.audiohandlers

import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager
import com.github.topisenpai.lavasrc.deezer.DeezerAudioSourceManager
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import main.audiohandlers.loaders.AudioLoaderKt
import main.audiohandlers.loaders.AutoPlayLoaderKt
import main.audiohandlers.loaders.SearchResultLoaderKt
import main.audiohandlers.sources.resume.ResumeSourceManagerKt
import main.constants.ENV
import main.constants.ToggleKt
import main.main.ConfigKt
import main.main.Robertify
import main.utils.RobertifyEmbedUtilsKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import main.utils.resume.ResumableTrackKt
import main.utils.resume.ResumableTrackKt.Companion.string
import main.utils.resume.ResumeDataKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.InteractionHook
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentSkipListMap

class RobertifyAudioManagerKt {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        val ins = RobertifyAudioManagerKt()
    }

    private val musicManagers = ConcurrentSkipListMap<Long, GuildMusicManagerKt>()
    val playerManager: AudioPlayerManager

    init {
        playerManager = DefaultAudioPlayerManager()

        AudioSourceManagers.registerLocalSource(playerManager)
        playerManager.registerSourceManager(ResumeSourceManagerKt(playerManager))
        playerManager.registerSourceManager(
            SpotifySourceManager(
                ConfigKt.providers,
                ConfigKt[ENV.SPOTIFY_CLIENT_ID],
                ConfigKt[ENV.SPOTIFY_CLIENT_SECRET],
                "us",
                playerManager
            )
        )
        playerManager.registerSourceManager(AppleMusicSourceManager(ConfigKt.providers, null, "us", playerManager))
        playerManager.registerSourceManager(DeezerAudioSourceManager(ConfigKt[ENV.DEEZER_ACCESS_TOKEN]))
        AudioSourceManagers.registerRemoteSources(playerManager)
    }

    fun getMusicManager(guild: Guild): GuildMusicManagerKt =
        musicManagers.computeIfAbsent(guild.idLong) { GuildMusicManagerKt(guild) }

    fun removeMusicManager(guild: Guild) {
        musicManagers[guild.idLong]?.destroy()
        musicManagers.remove(guild.idLong)
    }

    fun loadAndPlay(
        trackUrl: String,
        memberVoiceState: GuildVoiceState,
        botMessage: Message? = null,
        messageChannel: GuildMessageChannel? = null,
        addToBeginning: Boolean = false,
        shuffled: Boolean = false
    ) {
        if (!memberVoiceState.inAudioChannel())
            return

        val guild = memberVoiceState.guild
        val musicManager = getMusicManager(guild)
        val voiceChannel = memberVoiceState.channel!!

        if (voiceChannel.members.isNotEmpty()) {
            joinAudioChannel(voiceChannel, musicManager, messageChannel)
            loadTrack(
                trackUrl        = trackUrl,
                musicManager    = musicManager,
                user            = memberVoiceState.member.user,
                announceMessage = TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES),
                botMessage      =  botMessage,
                addToBeginning  = addToBeginning,
                shuffled        = shuffled
            )
        }
    }

    fun loadAndResume(musicManager: GuildMusicManagerKt, data: ResumeDataKt) {
        val channelId = data.channel_id
        // TODO: Change to Robertify Kotlin implementation
        val voiceChannel = Robertify.getShardManager().getVoiceChannelById(channelId)

        if (voiceChannel == null) {
            logger.warn("There was resume data for ${musicManager.guild.name} but the voice channel is invalid! Data: $data")
            return
        }

        if (voiceChannel.members.isEmpty())
            return

        joinAudioChannel(voiceChannel, musicManager)
        resumeTracks(data.tracks, musicManager.scheduler.announcementChannel, musicManager)
    }

    private fun resumeTracks(
        trackList: List<ResumableTrackKt>,
        announcementChannel: GuildMessageChannel?,
        musicManager: GuildMusicManagerKt
    ) {
        trackList.forEach { track ->
            val requester = track.requester
            if (requester != null)
                musicManager.scheduler.addRequester(requester.id, requester.trackId)
        }

        val trackUrl    = ResumeSourceManagerKt.SEARCH_PREFIX + trackList.string()
        val loader      = AudioLoaderKt(
            musicManager = musicManager,
            trackUrl = trackUrl,
            _announcementChannel = announcementChannel
        )
        musicManager.playerManager.loadItemOrdered(musicManager, trackUrl, loader)
    }

    private fun loadTrack(
        trackUrl: String,
        musicManager: GuildMusicManagerKt,
        user: User,
        announceMessage: Boolean = true,
        botMessage: Message?,
        addToBeginning: Boolean = false,
        shuffled: Boolean = false
    ) {
        val loader = AudioLoaderKt(
            sender          = user,
            trackUrl        = trackUrl,
            musicManager    = musicManager,
            addToBeginning  = addToBeginning,
            announceMsg     = announceMessage,
            botMsg          = botMessage,
            loadPlaylistShuffled = shuffled
        )
        musicManager.playerManager.loadItemOrdered(musicManager, trackUrl, loader)
    }

    private fun loadSearchResults(
        musicManager: GuildMusicManagerKt,
        searcher: User,
        botMessage: InteractionHook,
        query: String
    ) {
        val loader = SearchResultLoaderKt(
            guild       = musicManager.guild,
            query       = query,
            botMessage  = botMessage,
            searcher    = searcher
        )
        musicManager.playerManager.loadItemOrdered(musicManager, query, loader)
    }

    public fun loadRecommendedTracks(
        musicManager: GuildMusicManagerKt,
        channel: GuildMessageChannel?,
        trackIds: String
    ) {
        val loader = AutoPlayLoaderKt(musicManager, channel)
        musicManager.playerManager.loadItemOrdered(
            musicManager,
            "${SpotifySourceManager.RECOMMENDATIONS_PREFIX}seed_tracks=${trackIds}&limit=${30}",
            loader
        )
    }

    fun joinAudioChannel(
        channel: AudioChannel,
        musicManager: GuildMusicManagerKt,
        messageChannel: GuildMessageChannel? = null
    ) {
        try {
            check(channel.members.size == 0) { "I can't join a voice channel with no one in it!" }
            musicManager.link.connect(channel)
            musicManager.scheduler.scheduleDisconnect()
        } catch (e: InsufficientPermissionException) {
            messageChannel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    messageChannel.guild,
                    RobertifyLocaleMessageKt.GeneralMessages.INSUFFICIENT_PERMS_TO_JOIN,
                    Pair("{channel}", channel.asMention)
                ).build()
            )?.queue()
        }

    }
}