package main.audiohandlers

import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager
import com.github.topisenpai.lavasrc.deezer.DeezerAudioSourceManager
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.minn.jda.ktx.util.SLF4J
import lavalink.client.io.Link
import main.audiohandlers.loaders.AutoPlayLoaderKt
import main.audiohandlers.loaders.MainAudioLoaderKt
import main.audiohandlers.loaders.SearchResultLoaderKt
import main.audiohandlers.sources.resume.ResumeSourceManagerKt
import main.constants.ToggleKt
import main.main.Config
import main.main.RobertifyKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.editEmbed
import main.utils.json.guildconfig.GuildConfigKt
import main.utils.json.restrictedchannels.RestrictedChannelsConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
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
import java.util.*

object RobertifyAudioManagerKt {
    private val logger by SLF4J
    val musicManagers: MutableMap<Long, GuildMusicManagerKt> = Collections.synchronizedMap(mutableMapOf())
    val playerManager: AudioPlayerManager
    private val spotifySourceManager: SpotifySourceManager
    private val deezerAudioSourceManager: DeezerAudioSourceManager
    private val appleMusicSourceManager: AppleMusicSourceManager
    private val resumeSourceManager: ResumeSourceManagerKt

    init {
        playerManager = DefaultAudioPlayerManager()
        spotifySourceManager = SpotifySourceManager(
            Config.providers,
            Config.SPOTIFY_CLIENT_ID,
            Config.SPOTIFY_CLIENT_SECRET,
            "us",
            playerManager
        )
        deezerAudioSourceManager = DeezerAudioSourceManager(Config.DEEZER_ACCESS_TOKEN)
        appleMusicSourceManager = AppleMusicSourceManager(Config.providers, null, "us", playerManager)
        resumeSourceManager = ResumeSourceManagerKt(playerManager)

        AudioSourceManagers.registerLocalSource(playerManager)
        playerManager.registerSourceManager(resumeSourceManager)
        playerManager.registerSourceManager(spotifySourceManager)
        playerManager.registerSourceManager(deezerAudioSourceManager)
        playerManager.registerSourceManager(appleMusicSourceManager)
        AudioSourceManagers.registerRemoteSources(playerManager)
    }

    operator fun get(guild: Guild): GuildMusicManagerKt =
        musicManagers.computeIfAbsent(guild.idLong) {
            logger.debug("Creating new music manager for ${guild.name}")
            GuildMusicManagerKt(guild)
        }

    fun getMusicManager(guild: Guild): GuildMusicManagerKt = get(guild)

    fun removeMusicManager(guild: Guild) {
        musicManagers[guild.idLong]?.destroy()
        musicManagers.remove(guild.idLong)
    }

    fun loadAndPlay(
        trackUrl: String,
        memberVoiceState: GuildVoiceState,
        botMessage: Message? = null,
        addToBeginning: Boolean = false,
        shuffled: Boolean = false
    ) {
        if (!memberVoiceState.inAudioChannel())
            return

        val guild = memberVoiceState.guild
        val musicManager = getMusicManager(guild)
        val voiceChannel = memberVoiceState.channel!!

        if (voiceChannel.members.isNotEmpty() && joinAudioChannel(voiceChannel, musicManager, botMessage)) {
            loadTrack(
                trackUrl = trackUrl,
                musicManager = musicManager,
                user = memberVoiceState.member.user,
                announceMessage = TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES),
                botMessage = botMessage,
                addToBeginning = addToBeginning,
                shuffled = shuffled
            )
        }
    }

    fun loadAndResume(musicManager: GuildMusicManagerKt, data: ResumeDataKt) {
        val channelId = data.channel_id
        val voiceChannel = RobertifyKt.shardManager.getVoiceChannelById(channelId)

        if (voiceChannel == null) {
            logger.warn("There was resume data for ${musicManager.guild.name} but the voice channel is invalid! Data: $data")
            return
        }

        if (!GuildConfigKt(musicManager.guild).twentyFourSevenMode && voiceChannel.members.isEmpty())
            return

        if (joinAudioChannel(voiceChannel, musicManager))
            resumeTracks(data.tracks, musicManager.scheduler.announcementChannel, musicManager)
        else logger.warn("Could not resume tracks in ${musicManager.guild.name} because I couldn't join the voice channel!")
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

        val trackUrl = ResumeSourceManagerKt.SEARCH_PREFIX + trackList.string()
        MainAudioLoaderKt(
            musicManager = musicManager,
            query = trackUrl,
            _announcementChannel = announcementChannel
        ).loadItem()
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
        MainAudioLoaderKt(
            sender = user,
            query = trackUrl,
            musicManager = musicManager,
            addToBeginning = addToBeginning,
            announceMsg = announceMessage,
            botMsg = botMessage,
            loadPlaylistShuffled = shuffled
        ).loadItem()
    }

    fun loadSearchResults(
        musicManager: GuildMusicManagerKt,
        searcher: User,
        botMessage: InteractionHook,
        query: String
    ) {
        SearchResultLoaderKt(
            musicManager = musicManager,
            query = query,
            botMessage = botMessage,
            searcher = searcher
        ).loadItem()
    }

    fun loadRecommendedTracks(
        musicManager: GuildMusicManagerKt,
        channel: GuildMessageChannel?,
        trackIds: String
    ) {
        AutoPlayLoaderKt(
            musicManager,
            channel,
            "${SpotifySourceManager.RECOMMENDATIONS_PREFIX}seed_tracks=${trackIds}&limit=${30}"
        )
            .loadItem()
    }

    /**
     * Join a specified audio channel if the bot has permission to.
     *
     * @param channel The channel to attempt to join.
     * @param musicManager The music manager for the guild.
     * @param message The message to edit for any error messages.
     * @return True if the bot successfully joined the channel and vice-versa.
     */
    fun joinAudioChannel(
        channel: AudioChannel,
        musicManager: GuildMusicManagerKt,
        message: Message? = null,
        hookMessage: InteractionHook? = null
    ): Boolean {
        try {
            require(!GuildConfigKt(musicManager.guild).twentyFourSevenMode && channel.members.size > 0) { "I can't join a voice channel with no one in it!" }
            when (musicManager.link.state) {
                Link.State.DESTROYED, Link.State.NOT_CONNECTED -> {
                    val guild = musicManager.guild
                    if (TogglesConfigKt(guild).getToggle(ToggleKt.RESTRICTED_VOICE_CHANNELS)) {
                        val restrictedChannelConfig = RestrictedChannelsConfigKt(guild)
                        val localeManager = LocaleManagerKt[guild]

                        if (!restrictedChannelConfig.isRestrictedChannel(
                                channel.idLong,
                                RestrictedChannelsConfigKt.ChannelType.VOICE_CHANNEL
                            )
                        ) {
                            val embed = RobertifyEmbedUtilsKt.embedMessage(
                                guild,
                                """
                                    ${localeManager[GeneralMessages.CANT_JOIN_CHANNEL]}
                                    
                                    ${
                                    localeManager[
                                        GeneralMessages.RESTRICTED_TO_JOIN,
                                        Pair(
                                            "{channels}",
                                            if (restrictedChannelConfig.getRestrictedChannels(
                                                    RestrictedChannelsConfigKt.ChannelType.VOICE_CHANNEL
                                                )
                                                    .isNotEmpty()
                                            )
                                                restrictedChannelConfig.restrictedChannelsToString(
                                                    RestrictedChannelsConfigKt.ChannelType.VOICE_CHANNEL
                                                )
                                            else localeManager[GeneralMessages.NOTHING_HERE]
                                        )
                                    ]
                                }
                                """.trimIndent()
                            ).build()

                            if (message != null)
                                message.editEmbed { embed }.queue()
                            else hookMessage?.editEmbed(guild) { embed }?.queue()
                            return false
                        }
                    }

                    musicManager.link.connect(channel)
                    musicManager.scheduler.scheduleDisconnect()
                    return true
                }

                Link.State.CONNECTED, Link.State.CONNECTING -> return true
                else -> {
                    return false
                }
            }
        } catch (e: InsufficientPermissionException) {
            val embed = RobertifyEmbedUtilsKt.embedMessage(
                channel.guild,
                GeneralMessages.INSUFFICIENT_PERMS_TO_JOIN,
                Pair("{channel}", channel.asMention)
            ).build()
            if (message != null)
                message.editMessageEmbeds(embed).queue()
            else hookMessage?.editEmbed{ embed }?.queue()
        }

        return false
    }
}