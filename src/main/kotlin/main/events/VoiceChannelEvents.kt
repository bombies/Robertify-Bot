package main.events

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.GuildMusicManager
import main.audiohandlers.RobertifyAudioManager
import main.main.Robertify
import main.utils.json.guildconfig.GuildConfig
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent

class VoiceChannelEvents : AbstractEventController() {
    companion object {
        private val logger by SLF4J
    }

    private val onGuildVoiceUpdate =
        onEvent<GuildVoiceUpdateEvent> { event ->
            val guild = event.guild
            val self = guild.selfMember

            try {
                val channelLeft = event.channelLeft
                val channelJoined = event.channelJoined
                val selfVoiceState = self.voiceState!!
                val member = event.member
                val guildMusicManager = RobertifyAudioManager[guild]
                val guildDisconnector = guildMusicManager.scheduler.disconnectManager

                // If the bot has left voice channels entirely
                if (member.id == self.id && channelLeft != null && channelJoined == null) {
                    return@onEvent run {
                        guildDisconnector.cancelDisconnect()
                        guildMusicManager.clear()
                    }
                }

                if (!selfVoiceState.inAudioChannel())
                    return@onEvent

                val guildConfig = GuildConfig(guild)

                /*
                 * If the user left voice channels entirely or
                 * switched and the channel left is empty we want to
                 * disconnect the bot unless 24/7 mode is enabled.
                 */
                if (
                    (member.id != self.id
                            && ((channelJoined == null && channelLeft != null) || (channelJoined != null && channelLeft != null)))
                    && channelLeft.id == selfVoiceState.channel!!.id
                ) {
                    if (guildConfig.getTwentyFourSevenMode() || channelLeft.members.size > 1)
                        return@onEvent
                    guildMusicManager.player?.setPaused(true)?.subscribe()
                    if (!guildDisconnector.disconnectScheduled())
                        guildDisconnector.scheduleDisconnect()
                }

                /*
                 * If the user is joining a voice channel for the
                 * first time and the bot is awaiting disconnect,
                 * cancel the disconnect and resume playing the song if
                 * the song is paused.
                 */
                else if (member.id != self.id && channelJoined != null && channelJoined.id == selfVoiceState.channel!!.id) {
                    guildDisconnector.cancelDisconnect()
                    guildMusicManager.player?.setPaused(false)?.subscribe()
                }
            } catch (e: UninitializedPropertyAccessException) {
                logger.warn("Tried handling voice channel events before LavaLink was setup!")
            }
        }
}