package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.InteractionLimits
import main.utils.GeneralUtils.coerceAtMost
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.RemoveMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType

class RemoveCommand : AbstractSlashCommand(
    SlashCommand(
        name = "remove",
        description = "Remove a song from the queue.",
        subcommands = listOf(
            SubCommand(
                name = "id",
                description = "Remove a track from the queue by ID.",
                options = listOf(
                    CommandOption(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The ID of the track to remove from the queue."
                    )
                )
            ),
            SubCommand(
                name = "name",
                description = "Remove a track from the queue by name.",
                options = listOf(
                    CommandOption(
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

    private suspend fun handleRemove(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        id: Int
    ): MessageEmbed {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) return acChecks

        val guild = selfVoiceState.guild
        val queueHandler = RobertifyAudioManager[guild]
            .scheduler
            .queueHandler

        if (queueHandler.isEmpty)
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.NOTHING_IN_QUEUE)
                .build()

        if (id <= 0 || id > queueHandler.size)
            return RobertifyEmbedUtils.embedMessage(
                guild,
                RemoveMessages.REMOVE_INVALID_ID,
                Pair("{max}", queueHandler.size.toString())
            ).build()

        val trackList = queueHandler.contents
        val removedTrack = trackList[id - 1]

        return if (!queueHandler.remove(removedTrack))
            RobertifyEmbedUtils.embedMessage(
                guild,
                RemoveMessages.COULDNT_REMOVE,
                Pair("{id}", id.toString())
            ).build()
        else {
            LogUtilsKt(guild).sendLog(
                LogType.QUEUE_REMOVE,
                RemoveMessages.REMOVED_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", removedTrack.title),
                Pair("{author}", removedTrack.author)
            )
            if (id <= 10)
                RequestChannelConfig(guild).updateMessage()
            RobertifyEmbedUtils.embedMessage(
                guild,
                RemoveMessages.REMOVED,
                Pair("{title}", removedTrack.title),
                Pair("{author}", removedTrack.author)
            ).build()
        }
    }

    private suspend fun handleRemove(
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState,
        name: String
    ): MessageEmbed {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) return acChecks

        val guild = selfVoiceState.guild
        val queueHandler = RobertifyAudioManager[guild]
            .scheduler
            .queueHandler

        if (queueHandler.isEmpty)
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.NOTHING_IN_QUEUE)
                .build()

        val removedTrack =
            queueHandler.contents.firstOrNull { it.title.lowercase().startsWith(name.lowercase().trim()) }
                ?: return RobertifyEmbedUtils.embedMessage(
                    guild,
                    RemoveMessages.COULDNT_REMOVE_NAME,
                    Pair("{name}", name)
                )
                    .build()
        val id = queueHandler.contents.indexOf(removedTrack)

        return if (!queueHandler.remove(removedTrack))
            RobertifyEmbedUtils.embedMessage(
                guild,
                RemoveMessages.COULDNT_REMOVE_NAME,
                Pair("{name}", name)
            ).build()
        else {
            LogUtilsKt(guild).sendLog(
                LogType.QUEUE_REMOVE,
                RemoveMessages.REMOVED_LOG,
                Pair("{user}", memberVoiceState.member.asMention),
                Pair("{title}", removedTrack.title),
                Pair("{author}", removedTrack.author)
            )
            if (id < 10)
                RequestChannelConfig(guild).updateMessage()
            RobertifyEmbedUtils.embedMessage(
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
        val queueHandler = RobertifyAudioManager[guild].scheduler.queueHandler
        val queue = queueHandler.contents
        val search = event.focusedOption.value

        if (search.isEmpty())
            return event.replyChoices().queue()

        val options = queue.filter { it.title.lowercase().contains(search.lowercase().trim()) }
            .map {
                Choice(
                    "${it.title} by ${it.author}".coerceAtMost(InteractionLimits.COMMAND_OPTION_CHOICE_LENGTH),
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