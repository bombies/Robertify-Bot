package main.utils.pagination

import dev.minn.jda.ktx.interactions.components.secondary
import main.constants.MessageButton
import main.constants.RobertifyEmoji
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow

class PaginationButtonGenerator(
    private val frontEmoji: Emoji,
    private val previousEmoji: Emoji,
    private val nextEmoji: Emoji,
    private val endEmoji: Emoji,
) {
    companion object {
        val DEFAULT = PaginationButtonGenerator(
            RobertifyEmoji.PREVIOUS_EMOJI.emoji,
            RobertifyEmoji.REWIND_EMOJI.emoji,
            RobertifyEmoji.PLAY_EMOJI.emoji,
            RobertifyEmoji.END_EMOJI.emoji
        )
    }

    fun getButtons(
        user: User,
        frontEnabled: Boolean = true,
        previousEnabled: Boolean = true,
        nextEnabled: Boolean = true,
        endEnabled: Boolean = true,
        isQueue: Boolean = false
    ) =
        ActionRow.of(
            secondary(
                id = "${if (isQueue) "queue:" else ""}${MessageButton.FRONT}${user.id}",
                emoji = frontEmoji,
                disabled = !frontEnabled
            ),
            secondary(
                id = "${if (isQueue) "queue:" else ""}${MessageButton.PREVIOUS}${user.id}",
                emoji = previousEmoji,
                disabled = !previousEnabled
            ),
            secondary(
                id = "${if (isQueue) "queue:" else ""}${MessageButton.NEXT}${user.id}",
                emoji = nextEmoji,
                disabled = !nextEnabled
            ),
            secondary(
                id = "${if (isQueue) "queue:" else ""}${MessageButton.END}${user.id}",
                emoji = endEmoji,
                disabled = !endEnabled
            )
        )
}