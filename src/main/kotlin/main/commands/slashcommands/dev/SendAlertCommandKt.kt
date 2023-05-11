package main.commands.slashcommands.dev

import dev.minn.jda.ktx.interactions.components.*
import main.audiohandlers.loaders.MainAudioLoaderKt.Companion.queueThenDelete
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredValue
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

class SendAlertCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "sendalert",
        description = "Send an alert to all users",
        developerOnly = true
    )
) {
    companion object {
        private var cachedAlert: String? = null
    }


    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val textField = TextInput(
            id = "send_alert:alert",
            label = "Alert",
            placeholder = "The very important alert to send goes here!",
            style = TextInputStyle.PARAGRAPH
        )

        val modal = Modal(
            id = "send_alert",
            title = "New Alert",
            components = listOf(ActionRow.of(textField))
        )

        event.replyModal(modal).queue()
    }

    override suspend fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "send_alert") return

        val guild = event.guild!!
        val alert = event.getRequiredValue("send_alert:alert").asString
        cachedAlert = alert
        event.replyEmbed("Are you sure you want to send out this alert?\n\n${alert.replace("\\n", "\n")}")
            .setActionRow(
                success(
                    id = "sendalert:yes",
                    label = "Yes"
                ),
                danger(
                    id = "sendalert:no",
                    label = "No"
                ),
                secondary(
                    id = "sendalert:edit",
                    label = "Edit"
                )
            )
            .queue()
    }

    override suspend fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("sendalert:")) return

        val guild = event.guild!!
        val (_, action) = event.componentId.split(":")

        when (action) {
            "yes" -> {
                BotDBCacheKt.instance.latestAlert = Pair(cachedAlert!!, System.currentTimeMillis())
                event.replyEmbed("You have sent out a new alert!")
                    .queue()
            }

            "no" -> {
                event.replyEmbed("Okay! I will not send out that alert.")
                    .queue()
            }

            "edit" -> {
                val textField = TextInput(
                    id = "send_alert:alert",
                    label = "Alert",
                    placeholder = "The very important alert to send goes here!",
                    value = cachedAlert,
                    style = TextInputStyle.PARAGRAPH
                )

                val modal = Modal(
                    id = "send_alert",
                    title = "New Alert",
                    components = listOf(ActionRow.of(textField))
                )

                event.replyModal(modal).queue()
            }
        }

        event.message
            .editMessageComponents(
                ActionRow.of(
                    event.message
                        .buttons
                        .map { it.asDisabled() }
                )
            )
            .queueThenDelete(time = 1)
    }

}