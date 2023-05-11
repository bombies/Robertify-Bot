package main.commands.slashcommands.misc.polls

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.RobertifyPermissionKt
import main.constants.ToggleKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
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

class PollCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "polls",
        description = "Poll your community.",
        options = listOf(
            CommandOptionKt(
                name = "question",
                description = "The question to poll."
            ),
            CommandOptionKt(
                name = "choice1",
                description = "First choice"
            ),
            CommandOptionKt(
                name = "choice2",
                description = "Second choice"
            ),
            CommandOptionKt(
                name = "choice3",
                description = "Third choice",
                required = false
            ),
            CommandOptionKt(
                name = "choice4",
                description = "Fourth choice",
                required = false
            ),
            CommandOptionKt(
                name = "choice5",
                description = "Fifth choice",
                required = false
            ),
            CommandOptionKt(
                name = "choice6",
                description = "Sixth choice",
                required = false
            ),
            CommandOptionKt(
                name = "choice7",
                description = "Seventh choice",
                required = false
            ),
            CommandOptionKt(
                name = "choice8",
                description = "Eight choice",
                required = false
            ),
            CommandOptionKt(
                name = "choice9",
                description = "Ninth choice",
                required = false
            ),
            CommandOptionKt(
                name = "duration",
                description = "How long should the poll last?",
                required = false
            ),
        ),
        _requiredPermissions = listOf(RobertifyPermissionKt.ROBERTIFY_POLLS)
    )
) {

    companion object {
        internal val pollCache = mutableMapOf<Long, MutableMap<Int, Int>>()
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        if (!TogglesConfigKt(guild)[ToggleKt.POLLS])
            return event.replyEmbed(GeneralMessages.DISABLED_FEATURE)
                .setEphemeral(true)
                .queue()

        val choices = event.options
            .filter { it.name.contains("choice") }
            .mapNotNull { it.asString }
        val question = event.getRequiredOption("question").asString
        val durationStr = event.getOption("duration")?.asString
        val localeManager = LocaleManagerKt[guild]

        val endPoll = AtomicBoolean(false)
        val duration = AtomicLong(-1)
        var endTime = -1L

        if (durationStr != null && durationStr.matches("^[0-9]+[sSmMhHdD]$".toRegex())) {
            endPoll.set(true)

            try {
                endTime = GeneralUtilsKt.getFutureTime(durationStr)
                duration.set(GeneralUtilsKt.getStaticTime(durationStr))
            } catch (e: Exception) {
                return event.replyEmbed(
                    e.message ?: localeManager.getMessage(GeneralMessages.UNEXPECTED_ERROR)
                )
                    .setEphemeral(true)
                    .queue()
            }
        }

        val embedBuilder = RobertifyEmbedUtilsKt.embedMessage(guild, "\t")
        embedBuilder.setTitle(
            "**$question** ${
                if (endTime != -1L)
                    localeManager[
                        PollMessages.POLL_ENDS_AT,
                        Pair(
                            "{time}",
                            "<t:${endTime.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds}:t> (<t:${
                                endTime.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds
                            }:R>)"
                        )
                    ]
                else ""
            }\n\n"
        )

        embedBuilder.appendDescription(
            "${
                localeManager[PollMessages.POLL_BY, Pair(
                    "{user}",
                    event.user.asMention
                )]
            }\n\n"
        )

        choices.forEachIndexed { i, choice ->
            embedBuilder.appendDescription("${GeneralUtilsKt.parseNumEmoji(i + 1)} - *$choice*\n\n")
        }

        embedBuilder.setTimestamp(Instant.now())

        event.channel.sendEmbed { embedBuilder.build() }
            .queue { msg ->
                val map = mutableMapOf<Int, Int>()
                choices.forEachIndexed { i, _ ->
                    msg.addReaction(Emoji.fromFormatted(GeneralUtilsKt.parseNumEmoji(i + 1))).queue()
                    map[i] = 0
                }

                pollCache[msg.idLong] = map

                if (endPoll.get())
                    doPollEnd(msg, event.user, question, choices, duration.get())

                event.replyEmbed(PollMessages.POLL_SENT)
                    .setEphemeral(true)
                    .queue()

            }
    }

    private fun doPollEnd(msg: Message, user: User, question: String, choices: List<String>, timeToEnd: Long) {
        Executors.newSingleThreadScheduledExecutor()
            .schedule({
                val results = pollCache[msg.idLong]
                val winner = results!!.entries.maxBy { it.value }.key
                val guild = msg.guild
                val localeManager = LocaleManagerKt[guild]
                val embed = RobertifyEmbedUtilsKt.embedMessage(guild, localeManager[PollMessages.POLL_BY, Pair("{user}", user.asMention)])
                    .setTitle(localeManager[PollMessages.POLL_ENDED, Pair("{question}", question)])
                    .appendDescription("\n")
                    .addField(localeManager[PollMessages.POLL_WINNER_LABEL], "${choices[winner - 1]}\n\n", false)
                    .setTimestamp(Instant.now())
                    .build()

                msg.editMessageEmbeds(embed)
                    .queue { it.clearReactions().queue() }

                pollCache.remove(msg.idLong)
            }, timeToEnd, TimeUnit.MILLISECONDS)
    }

    override val help: String
        get() = """
                Want to ask your community members a question? This is the right tool for you. You are able to poll up to 9 choices with an optional time period.
                
                *NOTE: ROBERTIFY_POLLS is required to run this command.*"""
}