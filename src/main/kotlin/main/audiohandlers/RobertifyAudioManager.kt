package main.audiohandlers

import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager
import com.github.topisenpai.lavasrc.deezer.DeezerAudioSourceManager
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.minn.jda.ktx.util.SLF4J
import lavalink.client.io.Link
import main.audiohandlers.loaders.AutoPlayLoader
import main.audiohandlers.loaders.MainAudioLoader
import main.audiohandlers.loaders.SearchResultLoader
import main.audiohandlers.sources.resume.ResumeSourceManager
import main.constants.Toggle
import main.main.Config
import main.main.Robertify
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.editEmbed
import main.utils.json.guildconfig.GuildConfig
import main.utils.json.restrictedchannels.RestrictedChannelsConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.resume.ResumableTrack
import main.utils.resume.ResumableTrack.Companion.string
import main.utils.resume.ResumeData
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.*

object RobertifyAudioManager {
    private val logger by SLF4J
    val musicManagers: MutableMap<Long, GuildMusicManager> = Collections.synchronizedMap(mutableMapOf())
    val playerManager: AudioPlayerManager
    private val spotifySourceManager: SpotifySourceManager
    private val deezerAudioSourceManager: DeezerAudioSourceManager
    private val appleMusicSourceManager: AppleMusicSourceManager
    private val resumeSourceManager: ResumeSourceManager

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
        resumeSourceManager = ResumeSourceManager(playerManager)

        AudioSourceManagers.registerLocalSource(playerManager)
        playerManager.registerSourceManager(resumeSourceManager)
        playerManager.registerSourceManager(spotifySourceManager)
        playerManager.registerSourceManager(deezerAudioSourceManager)
        playerManager.registerSourceManager(appleMusicSourceManager)
        AudioSourceManagers.registerRemoteSources(playerManager)
    }

    operator fun get(guild: Guild): GuildMusicManager =
        musicManagers.computeIfAbsent(guild.idLong) {
            logger.debug("Creating new music manager for ${guild.name}")
            GuildMusicManager(guild)
        }

    fun getMusicManager(guild: Guild): GuildMusicManager = get(guild)

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
                announceMessage = TogglesConfig(guild).getToggle(Toggle.ANNOUNCE_MESSAGES),
                botMessage = botMessage,
                addToBeginning = addToBeginning,
                shuffled = shuffled
            )
        }
    }

    fun loadAndResume(musicManager: GuildMusicManager, data: ResumeData) {
        val channelId = data.channel_id
        val voiceChannel = Robertify.shardManager.getVoiceChannelById(channelId)

        if (voiceChannel == null) {
            logger.warn("There was resume data for ${musicManager.guild.name} but the voice channel is invalid! Data: $data")
            return
        }

        if (!GuildConfig(musicManager.guild).twentyFourSevenMode && voiceChannel.members.isEmpty())
            return

        if (joinAudioChannel(voiceChannel, musicManager))
            resumeTracks(data.tracks, musicManager.scheduler.announcementChannel, musicManager)
        else logger.warn("Could not resume tracks in ${musicManager.guild.name} because I couldn't join the voice channel!")
    }

    private fun resumeTracks(
        trackList: List<ResumableTrack>,
        announcementChannel: GuildMessageChannel?,
        musicManager: GuildMusicManager
    ) {
        trackList.forEach { track ->
            val requester = track.requester
            if (requester != null)
                musicManager.scheduler.addRequester(requester.id, requester.trackId)
        }

        val trackUrl = ResumeSourceManager.SEARCH_PREFIX + trackList.string()
        MainAudioLoader(
            musicManager = musicManager,
            query = trackUrl,
            _announcementChannel = announcementChannel
        ).loadItem()
    }

    private fun loadTrack(
        trackUrl: String,
        musicManager: GuildMusicManager,
        user: User,
        announceMessage: Boolean = true,
        botMessage: Message?,
        addToBeginning: Boolean = false,
        shuffled: Boolean = false
    ) {
        MainAudioLoader(
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
        musicManager: GuildMusicManager,
        searcher: User,
        botMessage: InteractionHook,
        query: String
    ) {
        SearchResultLoader(
            musicManager = musicManager,
            query = query,
            botMessage = botMessage,
            searcher = searcher
        ).loadItem()
    }

    fun loadRecommendedTracks(
        musicManager: GuildMusicManager,
        channel: GuildMessageChannel?,
        trackIds: String
    ) {
        AutoPlayLoader(
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
        musicManager: GuildMusicManager,
        message: Message? = null,
        hookMessage: InteractionHook? = null
    ): Boolean {
        try {
            require(!GuildConfig(musicManager.guild).twentyFourSevenMode && channel.members.size > 0) { "I can't join a voice channel with no one in it!" }
            when (musicManager.link.state) {
                Link.State.DESTROYED, Link.State.NOT_CONNECTED -> {
                    val guild = musicManager.guild
                    if (TogglesConfig(guild).getToggle(Toggle.RESTRICTED_VOICE_CHANNELS)) {
                        val restrictedChannelConfig = RestrictedChannelsConfig(guild)
                        val localeManager = LocaleManager[guild]

                        if (!restrictedChannelConfig.isRestrictedChannel(
                                channel.idLong,
                                RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                            )
                        ) {
                            val embed = RobertifyEmbedUtils.embedMessage(
                                guild,
                                """
                                    ${localeManager[GeneralMessages.CANT_JOIN_CHANNEL]}
                                    
                                    ${
                                    localeManager[
                                        GeneralMessages.RESTRICTED_TO_JOIN,
                                        Pair(
                                            "{channels}",
                                            if (restrictedChannelConfig.getRestrictedChannels(
                                                    RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                                )
                                                    .isNotEmpty()
                                            )
                                                restrictedChannelConfig.restrictedChannelsToString(
                                                    RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
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
            val embed = RobertifyEmbedUtils.embedMessage(
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