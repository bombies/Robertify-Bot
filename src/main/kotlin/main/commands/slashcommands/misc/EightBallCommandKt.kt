package main.commands.slashcommands.misc

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.BotConstantsKt
import main.constants.RobertifyPermissionKt
import main.utils.GeneralUtilsKt.hasPermissions
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.eightball.EightBallConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.EightBallMessages
import net.dv8tion.jda.api.entities.Invite.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.random.Random

class EightBallCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "8ball",
        description = "Curious of your fate?",
        subcommands = listOf(
            SubCommandKt(
                name = "add",
                description = "Add a custom response to 8ball.",
                options = listOf(
                    CommandOptionKt(
                        name = "response",
                        description = "The response to add."
                    )
                )
            ),
            SubCommandKt(
                name = "ask",
                description = "Ask 8ball a question",
                options = listOf(
                    CommandOptionKt(
                        name = "question",
                        description = "The question to ask."
                    )
                )
            ),
            SubCommandKt(
                name = "remove",
                description = "Remove a custom response from 8ball.",
                options = listOf(
                    CommandOptionKt(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The id of the response to remove.",
                        autoComplete = true
                    )
                )
            ),
            SubCommandKt(
                name = "clear",
                description = "Clear all custom responses from 8ball."
            ),
            SubCommandKt(
                name = "list",
                description = "List all custom 8ball responses."
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
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
        if (!member.hasPermissions(RobertifyPermissionKt.ROBERTIFY_8BALL))
            return event.replyEmbed(
                guild,
                BotConstantsKt.getInsufficientPermsMessage(guild, RobertifyPermissionKt.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        val config = EightBallConfigKt(guild)
        val response = event.getRequiredOption("response").asString

        if (config.responses.map { it.lowercase() }.contains(response.lowercase()))
            return event.replyEmbed(guild, EightBallMessages.ALREADY_A_RESPONSE).setEphemeral(true).queue()

        // Add the response
        config + response

        event.replyEmbed(guild, EightBallMessages.ADDED_RESPONSE, Pair("{response}", response)).setEphemeral(true)
            .queue()
    }

    private fun handleRemove(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermissionKt.ROBERTIFY_8BALL))
            return event.replyEmbed(
                guild,
                BotConstantsKt.getInsufficientPermsMessage(guild, RobertifyPermissionKt.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        val config = EightBallConfigKt(guild)
        val id = event.getRequiredOption("id").asInt - 1

        if (id < 0 || id >= config.responses.size)
            return event.replyEmbed(guild, EightBallMessages.NOT_A_RESPONSE).setEphemeral(true).queue()

        event.replyEmbed(
            guild,
            EightBallMessages.REMOVED_RESPONSE,
            // Remove the response
            Pair("{response}", config - id)
        )
            .setEphemeral(true)
            .queue()
    }

    private fun handleClear(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermissionKt.ROBERTIFY_8BALL))
            return event.replyEmbed(
                guild,
                BotConstantsKt.getInsufficientPermsMessage(guild, RobertifyPermissionKt.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        // Clear responses
        -EightBallConfigKt(guild)

        event.replyEmbed(guild, EightBallMessages.CLEARED_RESPONSES)
            .setEphemeral(true)
            .queue()
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        if (!member.hasPermissions(RobertifyPermissionKt.ROBERTIFY_8BALL))
            return event.replyEmbed(
                guild,
                BotConstantsKt.getInsufficientPermsMessage(guild, RobertifyPermissionKt.ROBERTIFY_8BALL)
            ).setEphemeral(true).queue()

        val config = EightBallConfigKt(guild)
        val responses = config.responses

        if (responses.isEmpty())
            return event.replyEmbed(guild, EightBallMessages.NO_CUSTOM_RESPONSES)
                .setEphemeral(true)
                .queue()

        val display = responses.mapIndexed { i, response ->
            "*$i* → $response"
        }.joinToString("\n")
        event.replyEmbed(guild, EightBallMessages.LIST_OF_RESPONSES, Pair("{responses}", display))
            .setEphemeral(true)
            .queue()
    }

    private fun handleAsk(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val localeManager = LocaleManagerKt[guild]

        val affirmativeAnswers = listOf(
            localeManager[EightBallMessages.EB_AF_1],
            localeManager[EightBallMessages.EB_AF_2],
            localeManager[EightBallMessages.EB_AF_3],
            localeManager[EightBallMessages.EB_AF_4],
            localeManager[EightBallMessages.EB_AF_5],
            localeManager[EightBallMessages.EB_AF_6],
            localeManager[EightBallMessages.EB_AF_7],
            localeManager[EightBallMessages.EB_AF_8],
            localeManager[EightBallMessages.EB_AF_9],
            localeManager[EightBallMessages.EB_AF_10],
        )

        val nonCommittalAnswers = listOf(
            localeManager[EightBallMessages.EB_NC_1],
            localeManager[EightBallMessages.EB_NC_2],
            localeManager[EightBallMessages.EB_NC_3],
            localeManager[EightBallMessages.EB_NC_4],
            localeManager[EightBallMessages.EB_NC_5],
        )

        val negativeAnswers = listOf(
            localeManager[EightBallMessages.EB_N_1],
            localeManager[EightBallMessages.EB_N_2],
            localeManager[EightBallMessages.EB_N_3],
            localeManager[EightBallMessages.EB_N_4],
            localeManager[EightBallMessages.EB_N_5],
        )

        val customAnswers = EightBallConfigKt(guild).responses
        val random = Random.nextDouble()
        val asker = event.user
        val question = event.getRequiredOption("question").asString

        val embed = RobertifyEmbedUtilsKt.embedMessage(
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

        event.replyEmbed(guild) { embed }.queue()
    }

    override suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "8ball" && event.focusedOption.name != "id") return

        if (!event.member!!.hasPermissions(RobertifyPermissionKt.ROBERTIFY_8BALL))
            return event.replyChoices().queue()

        val guild = event.guild!!
        val config = EightBallConfigKt(guild)
        val responses = config.responses
        val results = responses.filter { it.lowercase().contains(event.focusedOption.value.lowercase()) }
            .mapIndexed { i, response -> Choice(response.substring(0, response.length.coerceAtMost(100)), (i + 1).toLong()) }

        event.replyChoices(results).queue()
    }
}