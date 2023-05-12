package main.commands.slashcommands.dev

import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.TextInput
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.commands.slashcommands.SlashCommandManager.getRequiredValue
import main.constants.InteractionLimits
import main.utils.GeneralUtils.coerceAtMost
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.managers.RandomMessageManager
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

class RandomMessageCommand : AbstractSlashCommand(
    SlashCommand(
        name = "randommessage",
        description = "Configure the random messages for the bot.",
        developerOnly = true,
        subcommands = listOf(
            SubCommand(
                name = "add",
                description = "Add a random message."
            ),
            SubCommand(
                name = "remove",
                description = "Remove a random message.",
                options = listOf(
                    CommandOption(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The ID of the message to remove.",
                        autoComplete = true
                    )
                )
            ),
            SubCommand(
                name = "list",
                description = "List all random messages."
            ),
            SubCommand(
                name = "clear",
                description = "Clear all random messages."
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "add" -> handleAdd(event)
            "remove" -> handleRemove(event)
            "list" -> handleList(event)
            "clear" -> handleClear(event)
        }
    }

    private fun handleAdd(event: SlashCommandInteractionEvent) {
        val textField = TextInput(
            id = "random_message_modal:new_message",
            label = "New Message",
            style = TextInputStyle.PARAGRAPH,
            placeholder = "Enter a new random message to be sent"
        )
        val modal = Modal(
            id = "random_message:new",
            title = "New Random Message",
            components = listOf(ActionRow.of(textField))
        )

        event.replyModal(modal).queue()
    }

    private fun handleRemove(event: SlashCommandInteractionEvent) {
        val randomMessageManager = RandomMessageManager()
        val id = event.getRequiredOption("id").asInt - 1

        try {
            val removed = randomMessageManager - id
            event.replyEmbed("Successfully removed random message:\n\n$removed")
                .setEphemeral(true)
                .queue()
        } catch (e: IndexOutOfBoundsException) {
            event.replyEmbed(e.message ?: "Index out of bounds")
                .setEphemeral(true)
                .queue()
        }
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val randomMessageManager = RandomMessageManager()
        val messages = randomMessageManager.messages

        if (messages.isEmpty())
            return event.replyEmbed("There are no random messages")
                .setEphemeral(true)
                .queue()

        val desc = messages.mapIndexed { i, message ->
            "**$i**:\n```$message```"
        }.joinToString("\n")

        event.replyEmbed(desc).setEphemeral(true).queue()
    }

    private fun handleClear(event: SlashCommandInteractionEvent) {
        -RandomMessageManager()
        event.replyEmbed("Successfully cleared all messages!")
            .setEphemeral(true)
            .queue()
    }

    override suspend fun onModalInteraction(event: ModalInteractionEvent) {
        if (!event.modalId.startsWith("random_message:")) return
        val (_, modalType) = event.modalId.split(":")
        val randomMessageManager = RandomMessageManager()

        when (modalType) {
            "new" -> {
                val message = event.getRequiredValue("random_message_modal:new_message").asString
                randomMessageManager + message
                event.replyEmbed("Added a new random message:\n\n$message").setEphemeral(true).queue()
            }
        }
    }

    override suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "randommessage" && event.focusedOption.name != "id") return
        val randomMessageManager = RandomMessageManager()
        val choices = randomMessageManager.messages.mapIndexed { i, msg ->
            Choice(msg.substring(0, msg.length.coerceAtMost(100)), i.toLong() + 1)
        }.coerceAtMost(InteractionLimits.AUTO_COMPLETE_CHOICES)

        event.replyChoices(choices)
            .queue()
    }
}