package main.commands.slashcommands.audio

import main.audiohandlers.QueueHandler
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.MoveMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class MoveCommand : AbstractSlashCommand(
    SlashCommand(
        name = "move",
        description = "Rearrange the position of tracks in the queue.",
        options = listOf(
            CommandOption(
                type = OptionType.INTEGER,
                name = "id",
                description = "The ID of the track in the queue to move."
            ),
            CommandOption(
                type = OptionType.INTEGER,
                name = "position",
                description = "The position to move the track to."
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) {
            event.replyEmbed { acChecks }.setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()
        val queueHandler = RobertifyAudioManager[guild]
            .scheduler
            .queueHandler
        val id = event.getRequiredOption("id").asInt
        val pos = event.getRequiredOption("position").asInt

        event.hook.sendEmbed {
            handleMove(
                guild = guild,
                mover = event.user,
                queueHandler = queueHandler,
                id = id,
                position = pos
            )
        }.queue()
    }

    private fun handleMove(
        guild: Guild,
        mover: User,
        queueHandler: QueueHandler,
        id: Int,
        position: Int
    ): MessageEmbed {
        if (queueHandler.isEmpty)
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.NOTHING_IN_QUEUE)
                .build()

        val trackList = queueHandler.contents.toMutableList()

        if (id <= 0 || id > trackList.size)
            return RobertifyEmbedUtils.embedMessage(guild, MoveMessages.INVALID_SONG_ID)
                .build()
        else if (position <= 0 || position > trackList.size)
            return RobertifyEmbedUtils.embedMessage(guild, MoveMessages.INVALID_POSITION_ID)
                .build()

        val prevList = trackList.toMutableList()
        queueHandler.clear()
        prevList.remove(trackList[id - 1])
        prevList.add(position - 1, trackList[id - 1])

        if (!queueHandler.addAll(prevList)) {
            queueHandler.addAll(trackList)
            return RobertifyEmbedUtils.embedMessage(
                guild,
                MoveMessages.COULDNT_MOVE,
                Pair("{id}", id.toString())
            ).build()
        }

        RequestChannelConfig(guild).updateMessage()

        val movedTrack = trackList[id - 1]
        LogUtilsKt(guild).sendLog(
            LogType.TRACK_MOVE,
            MoveMessages.MOVED_LOG,
            Pair("{user}", mover.asMention),
            Pair("{title}", movedTrack.title),
            Pair("{author}", movedTrack.author),
            Pair("{position}", position.toString())
        )

        return RobertifyEmbedUtils.embedMessage(
            guild,
            MoveMessages.MOVED,
            Pair("{title}", movedTrack.title),
            Pair("{author}", movedTrack.author),
            Pair("{position}", position.toString())
        ).build()
    }

    override val help: String
        get() = """
                Move a specific track to a specific position in the queue

                Usage: `/move <id> <position>`"""

}