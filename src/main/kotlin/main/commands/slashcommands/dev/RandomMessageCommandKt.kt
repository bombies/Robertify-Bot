package main.commands.slashcommands.dev

import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.TextInput
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredValue
import main.constants.InteractionLimitsKt
import main.utils.GeneralUtilsKt.coerceAtMost
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.managers.RandomMessageManagerKt
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

class RandomMessageCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "randommessage",
        description = "Configure the random messages for the bot.",
        developerOnly = true,
        subcommands = listOf(
            SubCommandKt(
                name = "add",
                description = "Add a random message."
            ),
            SubCommandKt(
                name = "remove",
                description = "Remove a random message.",
                options = listOf(
                    CommandOptionKt(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The ID of the message to remove.",
                        autoComplete = true
                    )
                )
            ),
            SubCommandKt(
                name = "list",
                description = "List all random messages."
            ),
            SubCommandKt(
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
        val guild = event.guild!!
        val randomMessageManager = RandomMessageManagerKt()
        val id = event.getRequiredOption("id").asInt - 1

        try {
            val removed = randomMessageManager - id
            event.replyEmbed(guild, "Successfully removed random message:\n\n$removed")
                .setEphemeral(true)
                .queue()
        } catch (e: IndexOutOfBoundsException) {
            event.replyEmbed(guild, e.message ?: "Index out of bounds")
                .setEphemeral(true)
                .queue()
        }
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val randomMessageManager = RandomMessageManagerKt()
        val messages = randomMessageManager.messages

        if (messages.isEmpty())
            return event.replyEmbed(guild, "There are no random messages")
                .setEphemeral(true)
                .queue()

        val desc = messages.mapIndexed { i, message ->
            "**$i**:\n```$message```"
        }.joinToString("\n")

        event.replyEmbed(guild, desc).setEphemeral(true).queue()
    }

    private fun handleClear(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        -RandomMessageManagerKt()
        event.replyEmbed(guild, "Successfully cleared all messages!")
            .setEphemeral(true)
            .queue()
    }

    override suspend fun onModalInteraction(event: ModalInteractionEvent) {
        if (!event.modalId.startsWith("random_message:")) return
        val (_, modalType) = event.modalId.split(":")
        val randomMessageManager = RandomMessageManagerKt()
        val guild = event.guild!!

        when (modalType) {
            "new" -> {
                val message = event.getRequiredValue("random_message_modal:new_message").asString
                randomMessageManager + message
                event.replyEmbed(guild, "Added a new random message:\n\n$message").setEphemeral(true).queue()
            }
        }
    }

    override suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "randommessage" && event.focusedOption.name != "id") return
        val randomMessageManager = RandomMessageManagerKt()
        val choices = randomMessageManager.messages.mapIndexed { i, msg ->
            Choice(msg.substring(0, msg.length.coerceAtMost(100)), i.toLong() + 1)
        }.coerceAtMost(InteractionLimitsKt.AUTO_COMPLETE_CHOICES)

        event.replyChoices(choices)
            .queue()
    }
}