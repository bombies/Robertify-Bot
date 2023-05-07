package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.InteractionLimitsKt
import main.utils.GeneralUtilsKt.coerceAtMost
import main.utils.GeneralUtilsKt.isInt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.RemoveMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType

class RemoveCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "remove",
        description = "Remove a song from the queue.",
        subcommands = listOf(
            SubCommandKt(
                name = "id",
                description = "Remove a track from the queue by ID.",
                options = listOf(
                    CommandOptionKt(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The ID of the track to remove from the queue."
                    )
                )
            ),
            SubCommandKt(
                name = "name",
                description = "Remove a track from the queue by name.",
                options = listOf(
                    CommandOptionKt(
                        name = "name",
                        description = "The name of the track to remove",
                        autoComplete = true
                    )
                )
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = event.guild!!.selfMember.voiceState!!
        val (_, primaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (primaryCommand) {
            "id" -> {
                val id = event.getRequiredOption("id").asInt
                event.replyEmbed { handleRemove(memberVoiceState, selfVoiceState, id) }.queue()
            }

            "name" -> {
                val name = event.getRequiredOption("name").asString
                event.replyEmbed { handleRemove(memberVoiceState, selfVoiceState, name) }.queue()
            }
        }
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

    private fun handleRemove(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        name: String
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

        val removedTrack =
            queueHandler.contents.firstOrNull { it.title.lowercase().startsWith(name.lowercase().trim()) }
                ?: return RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RemoveMessages.COULDNT_REMOVE_NAME,
                    Pair("{name}", name)
                )
                    .build()
        val id = queueHandler.contents.indexOf(removedTrack)

        return if (!queueHandler.remove(removedTrack))
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RemoveMessages.COULDNT_REMOVE_NAME,
                Pair("{name}", name)
            ).build()
        else {
            LogUtilsKt(guild).sendLog(
                LogTypeKt.QUEUE_REMOVE,
                RemoveMessages.REMOVED_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", removedTrack.title),
                Pair("{author}", removedTrack.author)
            )
            if (id < 10)
                RequestChannelConfigKt(guild).updateMessage()
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RemoveMessages.REMOVED,
                Pair("{title}", removedTrack.title),
                Pair("{author}", removedTrack.author)
            ).build()
        }
    }

    override suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "remove" && event.focusedOption.name != "name") return

        val guild = event.guild!!
        val queueHandler = RobertifyAudioManagerKt[guild].scheduler.queueHandler
        val queue = queueHandler.contents
        val search = event.focusedOption.value

        if (search.isEmpty())
            return event.replyChoices().queue()

        val options = queue.filter { it.title.lowercase().contains(search.lowercase().trim()) }
            .map {
                Choice(
                    "${it.title} by ${it.author}".coerceAtMost(InteractionLimitsKt.COMMAND_OPTION_CHOICE_LENGTH),
                    it.title
                )
            }
            .coerceAtMost(25)

        event.replyChoices(options).queue()
    }

    override val help: String
        get() = """
                Remove a specific song from the queue

                Usage: `remove <id>`"""
}