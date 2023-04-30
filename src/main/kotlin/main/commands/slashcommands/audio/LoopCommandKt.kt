package main.commands.slashcommands.audio

import lavalink.client.player.IPlayer
import main.audiohandlers.GuildMusicManagerKt
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class LoopCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "loop",
        description = "Replay the current song being played.",
        subcommands = listOf(
            SubCommandKt(
                name = "track",
                description = "Toggle looping the song being currently played"
            ),
            SubCommandKt(
                name = "queue",
                description = "Toggle looping the current queue"
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val checksEmbed = checks(event.member!!.voiceState!!, event.guild!!.selfMember.voiceState!!, musicManager.player)
        if (checksEmbed != null) {
            event.replyWithEmbed { checksEmbed }
                .setEphemeral(true)
                .queue()
            return
        }

        when (event.subcommandName) {
            "track" -> event.replyWithEmbed { handleRepeat(musicManager, event.user) }.queue()
            "queue" -> event.replyWithEmbed { handleQueueRepeat(musicManager, event.user) }.queue()
        }
    }

    private fun checks(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState,
        player: IPlayer
    ): MessageEmbed? {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) return acChecks

        val guild = memberVoiceState.guild

        if (player.playingTrack == null)
            return RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.LoopMessages.LOOP_NOTHING_PLAYING)
                .build()

        return null
    }

    private fun handleRepeat(musicManager: GuildMusicManagerKt, looper: User): MessageEmbed {
        val guild = musicManager.guild
        val player = musicManager.player
        val scheduler = musicManager.scheduler
        val queueHandler = scheduler.queueHandler
        val track = player.playingTrack
            ?: return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING
            ).build()

        val embed = if (queueHandler.trackRepeating) {
            queueHandler.trackRepeating = false
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.LoopMessages.LOOP_STOP,
                Pair("{title}", track.title)
            ).build()
        } else {
            queueHandler.trackRepeating = true
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.LoopMessages.LOOP_START,
                Pair("{title}", track.title)
            ).build()
        }

        LogUtilsKt(guild).sendLog(
            LogTypeKt.TRACK_LOOP,
            RobertifyLocaleMessageKt.LoopMessages.LOOP_STOP,
            Pair("{user}", looper.asMention),
            Pair("{status}", if (queueHandler.trackRepeating) "looped" else "unlooped"),
            Pair("{title}", track.title),
            Pair("{author}", track.author)
        )

        return embed
    }

    private fun handleQueueRepeat(musicManager: GuildMusicManagerKt, looper: User): MessageEmbed {
        val scheduler = musicManager.scheduler
        val queueHandler = scheduler.queueHandler
        val guild = musicManager.guild
        val player = musicManager.player
        val embed = if (queueHandler.queueRepeating) {
            queueHandler.queueRepeating = false
            queueHandler.clearSavedQueue()
            RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.LoopMessages.QUEUE_LOOP_STOP).build()
        } else {
            if (player.playingTrack == null) {
                RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.GeneralMessages.NOTHING_PLAYING)
                    .build()
            } else {
                val thisTrack = player.playingTrack
                if (queueHandler.isEmpty)
                    RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.LoopMessages.QUEUE_LOOP_NOTHING)
                        .build()
                else {
                    queueHandler.queueRepeating = true
                    scheduler.addToBeginningOfQueue(thisTrack)
                    queueHandler.setSavedQueue(queueHandler.contents)
                    queueHandler.remove(thisTrack)
                    RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.LoopMessages.QUEUE_LOOP_START)
                        .build()
                }
            }
        }

        LogUtilsKt(guild).sendLog(
            LogTypeKt.TRACK_LOOP,
            RobertifyLocaleMessageKt.LoopMessages.QUEUE_LOOP_LOG,
            Pair("{user}", looper.asMention),
            Pair("{status}", if (queueHandler.queueRepeating) "looped" else "unlooped")
        )
        return embed
    }

    override val help: String
        get() = "Set the song being currently played or the queue to constantly loop"
}