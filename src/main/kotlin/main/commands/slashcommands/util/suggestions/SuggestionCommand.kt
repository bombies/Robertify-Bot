package main.commands.slashcommands.util.suggestions

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.TextInput
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.interactions.components.success
import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.util.SLF4J
import main.utils.GeneralUtils.digits
import main.utils.GeneralUtils.dm
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.SuggestionMessages
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import java.awt.Color
import java.time.Instant

class SuggestionCommand : AbstractSlashCommand(
    SlashCommand(
        name = "suggest",
        description = "Suggest a feature you'd like to see in the bot.",
        guildUseOnly = false
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val localeManager = LocaleManager[event.guild]

        val textFieldPlaceholder = localeManager.getMessage(SuggestionMessages.SUGGESTION_MODAL_PLACEHOLDER)
        val suggestion = TextInput(
            id = "suggestion:suggestion_field",
            label = localeManager.getMessage(SuggestionMessages.SUGGESTION_MODAL_LABEL),
            placeholder = textFieldPlaceholder.substring(0, textFieldPlaceholder.length.coerceAtMost(100)),
            style = TextInputStyle.PARAGRAPH,
        )

        event.replyModal(
            Modal(
                id = "suggestion:modal",
                title = localeManager.getMessage(SuggestionMessages.SUGGESTION_MODAL_TITLE),
                components = listOf(ActionRow.of(suggestion))
            )
        ).queue()
    }

    override suspend fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId == "suggestion:modal") handleSuggestionModal(event)
        else if (event.modalId.startsWith("suggestion_reason:")) handleReasonModal(event)
    }

    private suspend fun handleSuggestionModal(event: ModalInteractionEvent) {
        val guild = event.guild
        val suggestion = event.getValue("suggestion:suggestion_field")?.asString
            ?: return event.replyEmbed(SuggestionMessages.INVALID_SUGGESTION).queue()

        val config = BotDBCache.instance
        val pendingChannel = event.jda.shardManager!!.getTextChannelById(config.suggestionsPendingChannelId)

        if (!config.suggestionSetup) {
            logger.warn("The suggestion channels aren't setup!")
            return event.replyEmbed(GeneralMessages.FEATURE_UNAVAILABLE)
                .setEphemeral(true)
                .queue()
        }

        if (config.userIsSuggestionBanned(event.user.idLong))
            return event.replyEmbed(SuggestionMessages.SUGGESTION_USER_BANNED)
                .setEphemeral(true)
                .queue()

        val suggester = event.user
        val embed = getGenericSuggestionEmbed(
            title = "Suggestion from ${suggester.name}",
            color = Color(255, 196, 0),
            suggester = suggester,
            suggestion = suggestion
        )

        pendingChannel!!.sendEmbed { embed }
            .setActionRow(
                success(
                    id = "suggestion_button:accept",
                    label = "Accept"
                ),
                danger(
                    id = "suggestion_button:deny",
                    label = "Deny"
                )
            )
            .queue()

        return event.replyEmbed(SuggestionMessages.SUGGESTION_SUBMITTED)
            .setEphemeral(true)
            .queue()
    }

    private suspend fun handleReasonModal(event: ModalInteractionEvent) {
        val config = BotDBCache.instance
        val guild = event.guild!!
        val localeManager = LocaleManager[guild]
        val (_, reasonType, messageId) = event.modalId.split(":")
        val reason = event.getValue("suggestion:reason_field")?.asString ?: if (reasonType == "accept")
            localeManager.getMessage(SuggestionMessages.DEFAULT_ACCEPT_REASON)
        else localeManager.getMessage(SuggestionMessages.DEFAULT_DENY_REASON)

        val shardManager = event.jda.shardManager!!
        val pendingChannel = shardManager.getTextChannelById(config.suggestionsPendingChannelId)!!
        event.deferReply(true).queue()

        val suggestionMessage = pendingChannel.retrieveMessageById(messageId).await()
        val suggester = shardManager.getUserById(suggestionMessage.embeds.first().fields.first().value!!.digits())
        val pendingSuggestion = suggestionMessage.embeds.first().description!!

        when (reasonType) {
            "accept" -> {
                val acceptedChannel = shardManager.getTextChannelById(config.suggestionsAcceptedChannelId)
                if (acceptedChannel == null) {
                    logger.warn("The accepted suggestion channel isn't setup!")
                    return event.replyEmbed(GeneralMessages.FEATURE_UNAVAILABLE)
                        .setEphemeral(true)
                        .queue()
                }

                acceptedChannel.sendMessageEmbeds(
                    getGenericSuggestionEmbed(
                        title = "Suggestion",
                        color = Color(77, 255, 69),
                        suggester = suggester,
                        suggestion = pendingSuggestion,
                        reason = reason
                    )
                ).queue()

                suggestionMessage.delete().queue()

                suggester?.dm(
                    EmbedBuilder(
                        title = "Suggestions",
                        color = Color(77, 255, 69).rgb,
                        description = "**Your suggestion has been accepted!**" +
                                "\nYou will see it appear in the next changelog\n\n**Your suggestion**\n```${
                                    pendingSuggestion.substring(
                                        0,
                                        pendingSuggestion.length.coerceAtMost(3900)
                                    )
                                }```",
                        fields = listOf(
                            MessageEmbed.Field("Comments", reason, false)
                        )
                    ).build()
                )

                event.hook.sendEmbed(guild, "Successfully accepted the suggestion!")
                    .queue()
            }

            "deny" -> {
                val deniedChannel = shardManager.getTextChannelById(config.suggestionsDeniedChannelId)
                if (deniedChannel == null) {
                    logger.warn("The denied suggestion channel isn't setup!")
                    return event.replyEmbed(GeneralMessages.FEATURE_UNAVAILABLE)
                        .setEphemeral(true)
                        .queue()
                }

                deniedChannel.sendMessageEmbeds(
                    getGenericSuggestionEmbed(
                        title = "Suggestion",
                        color = Color(187, 0, 0),
                        suggester = suggester,
                        suggestion = pendingSuggestion,
                        reason = reason
                    )
                ).queue()

                suggestionMessage.delete().queue()

                suggester?.dm(
                    EmbedBuilder(
                        title = "Suggestions",
                        color = Color(187, 0, 0).rgb,
                        description = "**Your suggestion has been rejected.**\n\n" +
                                "**Your suggestion**\n" +
                                "```${
                                    pendingSuggestion.substring(
                                        0,
                                        pendingSuggestion.length.coerceAtMost(3900)
                                    )
                                }```",
                        fields = listOf(
                            MessageEmbed.Field("Comments", reason, false)
                        )
                    ).build()
                )

                event.hook.sendEmbed(guild, "Successfully denied the suggestion!")
                    .queue()
            }
        }
    }

    override suspend fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("suggestion_button:")) return

        val config = BotDBCache.instance
        val guild = event.guild
        if (!config.isDeveloper(event.user.idLong))
            return event.replyEmbed(GeneralMessages.NO_PERMS_BUTTON)
                .setEphemeral(true)
                .queue()

        val messageId = event.messageId
        val reasonModalText = TextInput(
            id = "suggestion:reason_field",
            label = "Reason",
            placeholder = "Fancy reason goes here",
            requiredLength = 0..1024,
            required = false,
            style = TextInputStyle.PARAGRAPH
        )

        val reasonModal = Modal(
            id = "suggestion_reason:${event.componentId.split(":")[1]}:$messageId",
            title = "Provide A Reason",
            components = listOf(ActionRow.of(reasonModalText))
        )

        event.replyModal(reasonModal).queue()
    }

    private fun getGenericSuggestionEmbed(
        title: String,
        color: Color,
        suggester: User?,
        suggestion: String,
        reason: String? = null
    ): MessageEmbed =
        EmbedBuilder(
            title = title,
            fields = listOf(
                MessageEmbed.Field("Suggester", suggester?.asMention ?: "Unknown Suggester", false),
            ) + if (reason != null) listOf(
                MessageEmbed.Field("Reason", reason, false)
            ) else listOf(),
            description = suggestion,
            color = color.rgb,
            timestamp = Instant.now()
        ).build()

}