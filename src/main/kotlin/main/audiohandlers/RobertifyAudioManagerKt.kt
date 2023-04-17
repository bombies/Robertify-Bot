package main.audiohandlers

import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager
import com.github.topisenpai.lavasrc.deezer.DeezerAudioSourceManager
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import main.constants.ENV
import main.main.ConfigKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.util.concurrent.ConcurrentSkipListMap

class RobertifyAudioManagerKt {
    companion object {
        val ins = RobertifyAudioManagerKt()
        val musicManagers = ConcurrentSkipListMap<Long, GuildMusicManagerKt>()
    }

    val playerManager: AudioPlayerManager

    init {
        playerManager = DefaultAudioPlayerManager()

        AudioSourceManagers.registerLocalSource(playerManager)
        // TODO: Implement ResumeSourceManager Kotlin implementation
        playerManager.registerSourceManager(SpotifySourceManager(ConfigKt.providers, ConfigKt[ENV.SPOTIFY_CLIENT_ID], ConfigKt[ENV.SPOTIFY_CLIENT_SECRET], "us", playerManager))
        playerManager.registerSourceManager(AppleMusicSourceManager(ConfigKt.providers, null, "us", playerManager))
        playerManager.registerSourceManager(DeezerAudioSourceManager(ConfigKt[ENV.DEEZER_ACCESS_TOKEN]))
        AudioSourceManagers.registerRemoteSources(playerManager)
    }

    fun getMusicManager(guild: Guild): GuildMusicManagerKt =
        musicManagers.computeIfAbsent(guild.idLong) { GuildMusicManagerKt(guild)}

    fun removeMusicManager(guild: Guild) {
        musicManagers[guild.idLong]?.destroy()
        musicManagers.remove(guild.idLong)
    }

    fun joinAudioChannel(channel: AudioChannel, musicManager: GuildMusicManagerKt, messageChannel: GuildMessageChannel? = null) {
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