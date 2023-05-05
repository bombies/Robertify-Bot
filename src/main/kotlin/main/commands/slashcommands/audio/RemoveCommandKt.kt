package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.RemoveMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class RemoveCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "remove",
        description = "Remove a song from the queue.",
        options = listOf(
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "id",
                description = "The ID of the track to remove from the queue."
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = event.guild!!.selfMember.voiceState!!
        val id = event.getRequiredOption("id").asInt
        event.replyWithEmbed { handleRemove(memberVoiceState, selfVoiceState, id) }.queue()
    }

    private fun handleRemove(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        id: Int
    ): MessageEmbed {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) return acChecks

        val guild = selfVoiceState.guild
        val queueHandler = RobertifyAudioManagerKt[guild]
            .scheduler
            .queueHandler

        if (queueHandler.isEmpty)
            return RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.NOTHING_IN_QUEUE)
                .build()

        if (id <= 0 || id > queueHandler.size)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RemoveMessages.REMOVE_INVALID_ID,
                Pair("{max}", queueHandler.size.toString())
            ).build()

        val trackList = queueHandler.contents
        val removedTrack = trackList[id - 1]

        return if (!queueHandler.remove(removedTrack))
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RemoveMessages.COULDNT_REMOVE,
                Pair("{id}", id.toString())
            ).build()
        else {
            LogUtilsKt(guild).sendLog(
                LogTypeKt.QUEUE_REMOVE,
                RemoveMessages.REMOVED_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", removedTrack.title),
                Pair("{author}", removedTrack.author)
            )
            if (id <= 10)
                RequestChannelConfigKt(guild).updateMessage()
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RemoveMessages.REMOVED,
                Pair("{title}", removedTrack.title),
                Pair("{author}", removedTrack.author)
            ).build()
        }
    }

    override val help: String
        get() = """
                Remove a specific song from the queue

                Usage: `remove <id>`"""
}