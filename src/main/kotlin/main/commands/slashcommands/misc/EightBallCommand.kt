package main.commands.slashcommands.misc

import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.BotConstants
import main.constants.InteractionLimits
import main.constants.RobertifyPermission
import main.utils.GeneralUtils.coerceAtMost
import main.utils.GeneralUtils.hasPermissions
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.json.eightball.EightBallConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.EightBallMessages
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.random.Random

class EightBallCommand : AbstractSlashCommand(
    SlashCommand(
        name = "8ball",
        description = "Curious of your fate?",
        subcommands = listOf(
            SubCommand(
                name = "add",
                description = "Add a custom response to 8ball.",
                options = listOf(
                    CommandOption(
                        name = "response",
                        description = "The response to add."
                    )
                )
            ),
            SubCommand(
                name = "ask",
                description = "Ask 8ball a question",
                options = listOf(
                    CommandOption(
                        name = "question",
                        description = "The question to ask."
                    )
                )
            ),
            SubCommand(
                name = "remove",
                description = "Remove a custom response from 8ball.",
                options = listOf(
                    CommandOption(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The id of the response to remove.",
                        autoComplete = true
                    )
                )
            ),
            SubCommand(
                name = "clear",
                description = "Clear all custom responses from 8ball."
            ),
            SubCommand(
                name = "list",
                description = "List all custom 8ball responses."
            )
        )
    )
) {

    override fun handle(event: SlashCommandInteractionEvent) {
        val (_, primaryCommand) = event.fullCommandName.split("\\s".toRegex())

        when (primaryCommand) {
            "add" -> handleAdd(event)
            "ask" -> handleAsk(event)
            "remove" -> handleRemove(event)
            "clear" -> handleClear(event)
            "list" -> handleList(event)
        }
    }

    private fun handleAdd(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermission.ROBERTIFY_8BALL))
            return event.replyEmbed(
                BotConstants.getInsufficientPermsMessage(guild, RobertifyPermission.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        val config = EightBallConfig(guild)
        val response = event.getRequiredOption("response").asString

        if (config.getResponses().map { it.lowercase() }.contains(response.lowercase()))
            return event.replyEmbed(EightBallMessages.ALREADY_A_RESPONSE).setEphemeral(true).queue()

        // Add the response
        config.addResponse(response)

        event.replyEmbed(EightBallMessages.ADDED_RESPONSE, Pair("{response}", response)).setEphemeral(true)
            .queue()
    }

    private fun handleRemove(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermission.ROBERTIFY_8BALL))
            return event.replyEmbed(
                BotConstants.getInsufficientPermsMessage(guild, RobertifyPermission.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        val config = EightBallConfig(guild)
        val id = event.getRequiredOption("id").asInt - 1

        if (id < 0 || id >= config.getResponses().size)
            return event.replyEmbed(EightBallMessages.NOT_A_RESPONSE).setEphemeral(true).queue()

        event.replyEmbed(
            EightBallMessages.REMOVED_RESPONSE,
            // Remove the response
            Pair("{response}", config.removeResponse(id))
        )
            .setEphemeral(true)
            .queue()
    }

    private fun handleClear(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermission.ROBERTIFY_8BALL))
            return event.replyEmbed(
                BotConstants.getInsufficientPermsMessage(guild, RobertifyPermission.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        // Clear responses
        EightBallConfig(guild).clearResponses()

        event.replyEmbed(EightBallMessages.CLEARED_RESPONSES)
            .setEphemeral(true)
            .queue()
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermission.ROBERTIFY_8BALL))
            return event.replyEmbed(
                BotConstants.getInsufficientPermsMessage(guild, RobertifyPermission.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        val config = EightBallConfig(guild)
        val responses = config.getResponses()

        if (responses.isEmpty())
            return event.replyEmbed(EightBallMessages.NO_CUSTOM_RESPONSES)
                .setEphemeral(true)
                .queue()

        val display = responses.mapIndexed { i, response ->
            "*$i* → $response"
        }.joinToString("\n")
        event.replyEmbed(EightBallMessages.LIST_OF_RESPONSES, Pair("{responses}", display))
            .setEphemeral(true)
            .queue()
    }

    private fun handleAsk(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val localeManager = LocaleManager[guild]

        val affirmativeAnswers = listOf(
            localeManager.getMessage(EightBallMessages.EB_AF_1),
            localeManager.getMessage(EightBallMessages.EB_AF_2),
            localeManager.getMessage(EightBallMessages.EB_AF_3),
            localeManager.getMessage(EightBallMessages.EB_AF_4),
            localeManager.getMessage(EightBallMessages.EB_AF_5),
            localeManager.getMessage(EightBallMessages.EB_AF_6),
            localeManager.getMessage(EightBallMessages.EB_AF_7),
            localeManager.getMessage(EightBallMessages.EB_AF_8),
            localeManager.getMessage(EightBallMessages.EB_AF_9),
            localeManager.getMessage(EightBallMessages.EB_AF_10),
        )

        val nonCommittalAnswers = listOf(
            localeManager.getMessage(EightBallMessages.EB_NC_1),
            localeManager.getMessage(EightBallMessages.EB_NC_2),
            localeManager.getMessage(EightBallMessages.EB_NC_3),
            localeManager.getMessage(EightBallMessages.EB_NC_4),
            localeManager.getMessage(EightBallMessages.EB_NC_5),
        )

        val negativeAnswers = listOf(
            localeManager.getMessage(EightBallMessages.EB_N_1),
            localeManager.getMessage(EightBallMessages.EB_N_2),
            localeManager.getMessage(EightBallMessages.EB_N_3),
            localeManager.getMessage(EightBallMessages.EB_N_4),
            localeManager.getMessage(EightBallMessages.EB_N_5),
        )

        val customAnswers = EightBallConfig(guild).getResponses()
        val random = Random.nextDouble()
        val asker = event.user
        val question = event.getRequiredOption("question").asString

        val embed = RobertifyEmbedUtils.embedMessage(
            guild,
            EightBallMessages.QUESTION_ASKED,
            Pair("{user}", asker.asMention),
            Pair("{question}", question),
            Pair(
                "{response}", when {
                    customAnswers.isNotEmpty() -> {
                        when (random) {
                            in 0.0..0.11 -> affirmativeAnswers[Random.nextInt(affirmativeAnswers.size)]
                            in 0.12..0.22 -> nonCommittalAnswers[Random.nextInt(nonCommittalAnswers.size)]
                            in 0.23..0.33 -> negativeAnswers[Random.nextInt(negativeAnswers.size)]
                            in 0.34..1.0 -> customAnswers[Random.nextInt(customAnswers.size)]
                            else -> throw IllegalArgumentException("Something impossible happened")
                        }
                    }

                    else -> {
                        when (random) {
                            in 0.0..0.5 -> affirmativeAnswers[Random.nextInt(affirmativeAnswers.size)]
                            in 0.51..0.75 -> nonCommittalAnswers[Random.nextInt(nonCommittalAnswers.size)]
                            in 0.76..1.0 -> negativeAnswers[Random.nextInt(negativeAnswers.size)]
                            else -> throw IllegalArgumentException("Something impossible happened")
                        }
                    }
                }
            )
        ).build()

        event.replyEmbed { embed }.queue()
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "8ball" && event.focusedOption.name != "id") return

        if (!event.member!!.hasPermissions(RobertifyPermission.ROBERTIFY_8BALL))
            return event.replyChoices().queue()

        val guild = event.guild!!
        val config = EightBallConfig(guild)
        val responses = config.getResponses()
        val results = responses.filter { it.lowercase().contains(event.focusedOption.value.lowercase()) }
            .mapIndexed { i, response ->
                Choice(
                    response.substring(
                        0,
                        response.length.coerceAtMost(InteractionLimits.COMMAND_OPTION_CHOICE_LENGTH)
                    ), (i + 1).toLong()
                )
            }.coerceAtMost(25)

        event.replyChoices(results).queue()
    }
}