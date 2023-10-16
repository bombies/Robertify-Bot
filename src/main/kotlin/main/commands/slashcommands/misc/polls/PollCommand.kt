package main.commands.slashcommands.misc.polls

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.runBlocking
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.RobertifyPermission
import main.constants.Toggle
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PollMessages
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PollCommand : AbstractSlashCommand(
    SlashCommand(
        name = "polls",
        description = "Poll your community.",
        options = listOf(
            CommandOption(
                name = "question",
                description = "The question to poll."
            ),
            CommandOption(
                name = "choice1",
                description = "First choice"
            ),
            CommandOption(
                name = "choice2",
                description = "Second choice"
            ),
            CommandOption(
                name = "choice3",
                description = "Third choice",
                required = false
            ),
            CommandOption(
                name = "choice4",
                description = "Fourth choice",
                required = false
            ),
            CommandOption(
                name = "choice5",
                description = "Fifth choice",
                required = false
            ),
            CommandOption(
                name = "choice6",
                description = "Sixth choice",
                required = false
            ),
            CommandOption(
                name = "choice7",
                description = "Seventh choice",
                required = false
            ),
            CommandOption(
                name = "choice8",
                description = "Eight choice",
                required = false
            ),
            CommandOption(
                name = "choice9",
                description = "Ninth choice",
                required = false
            ),
            CommandOption(
                name = "duration",
                description = "How long should the poll last?",
                required = false
            ),
        ),
        _requiredPermissions = listOf(RobertifyPermission.ROBERTIFY_POLLS)
    )
) {

    companion object {
        internal val pollCache = mutableMapOf<Long, MutableMap<Int, Int>>()
        private val logger by SLF4J
    }

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        if (!TogglesConfig(guild).get(Toggle.POLLS))
            return event.replyEmbed(GeneralMessages.DISABLED_FEATURE)
                .setEphemeral(true)
                .queue()

        val choices = event.options
            .filter { it.name.contains("choice") }
            .mapNotNull { it.asString }
        val question = event.getRequiredOption("question").asString
        val durationStr = event.getOption("duration")?.asString
        val localeManager = LocaleManager[guild]

        var endPoll = false
        var duration = -1L
        var endTime = -1L

        if (durationStr != null && durationStr.matches("^[0-9]+[sSmMhHdD]$".toRegex())) {
            endPoll = true

            try {
                endTime = GeneralUtils.getFutureTime(durationStr)
                duration = GeneralUtils.getStaticTime(durationStr)
            } catch (e: Exception) {
                return event.replyEmbed(
                    e.message ?: localeManager.getMessage(GeneralMessages.UNEXPECTED_ERROR)
                )
                    .setEphemeral(true)
                    .queue()
            }
        }

        val embedBuilder = RobertifyEmbedUtils.embedMessage(guild, "\t")
        embedBuilder.setTitle(
            "**$question** ${
                if (endTime != -1L)
                    localeManager.getMessage(
                        PollMessages.POLL_ENDS_AT,
                        Pair(
                            "{time}",
                            "<t:${endTime.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds}:t> (<t:${
                                endTime.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds
                            }:R>)"
                        )
                    )
                else ""
            }\n\n"
        )

        embedBuilder.appendDescription(
            "${
                localeManager.getMessage(
                    PollMessages.POLL_BY, Pair(
                        "{user}",
                        event.user.asMention
                    )
                )
            }\n\n"
        )

        choices.forEachIndexed { i, choice ->
            embedBuilder.appendDescription("${GeneralUtils.parseNumEmoji(i + 1)} - *$choice*\n\n")
        }

        embedBuilder.setTimestamp(Instant.now())

        event.channel.sendEmbed { embedBuilder.build() }.queue { msg ->
            val map = mutableMapOf<Int, Int>()
            choices.forEachIndexed { i, _ ->
                msg.addReaction(Emoji.fromFormatted(GeneralUtils.parseNumEmoji(i + 1))).queue()
                map[i] = 0
            }

            pollCache[msg.idLong] = map

            if (endPoll)
                doPollEnd(msg, event.user, question, choices, duration)

            event.replyEmbed(PollMessages.POLL_SENT)
                .setEphemeral(true)
                .queue()
        }
    }

    private fun doPollEnd(msg: Message, user: User, question: String, choices: List<String>, timeToEnd: Long) {
        Executors.newSingleThreadScheduledExecutor()
            .schedule({
                runBlocking {
                    val results = pollCache[msg.idLong]
                    val winner = results!!.entries.maxBy { it.value }.key
                    val guild = msg.guild
                    val localeManager = LocaleManager[guild]
                    val embed = RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(PollMessages.POLL_BY, Pair("{user}", user.asMention))
                    )
                        .setTitle(localeManager.getMessage(PollMessages.POLL_ENDED, Pair("{question}", question)))
                        .appendDescription("\n")
                        .addField(
                            localeManager.getMessage(PollMessages.POLL_WINNER_LABEL),
                            "${choices[winner]}\n\n",
                            false
                        )
                        .setTimestamp(Instant.now())
                        .build()

                    msg.editMessageEmbeds(embed)
                        .queue { it.clearReactions().queue() }

                    pollCache.remove(msg.idLong)
                }
            }, timeToEnd, TimeUnit.MILLISECONDS)
    }

    override val help: String
        get() = """
                Want to ask your community members a question? This is the right tool for you. You are able to poll up to 9 choices with an optional time period.
                
                *NOTE: ROBERTIFY_POLLS is required to run this command.*"""
}