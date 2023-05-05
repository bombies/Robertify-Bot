package main.commands.slashcommands.util

import dev.minn.jda.ktx.interactions.components.link
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class VoteCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "vote",
    description = "Want to support us? Help spread our reach by voting for us!",
    guildUseOnly = false
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyWithEmbed(event.guild, GeneralMessages.VOTE_EMBED_DESC)
            .setActionRow(
                link(
                    url = "https://top.gg/bot/893558050504466482/vote",
                    label = "Top.gg"
                ),
                link(
                    url = "https://discordbotlist.com/bots/robertify/upvote",
                    label = "Discord Bot List"
                )
            )
            .queue()
    }

    override val help: String
        get() = "Do you like Robertify and want to help us share it with more users? Do us the favour of voting for us! " +
                "It would really help us in growing our reach. 💖"
}