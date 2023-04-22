package main.utils.pagination

import dev.minn.jda.ktx.interactions.components.Paginator
import dev.minn.jda.ktx.interactions.components.secondary
import main.constants.MessageButtonKt
import main.constants.RobertifyEmojiKt
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
        val default = PaginationButtonGenerator(
            RobertifyEmojiKt.PREVIOUS_EMOJI.emoji,
            RobertifyEmojiKt.REWIND_EMOJI.emoji,
            RobertifyEmojiKt.PLAY_EMOJI.emoji,
            RobertifyEmojiKt.END_EMOJI.emoji
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
                id = "${if (isQueue) "queue:" else ""}${MessageButtonKt.FRONT}${user.id}",
                emoji = frontEmoji,
                disabled = !frontEnabled
            ),
            secondary(
                id = "${if (isQueue) "queue:" else ""}${MessageButtonKt.PREVIOUS}${user.id}",
                emoji = previousEmoji,
                disabled = !previousEnabled
            ),
            secondary(
                id = "${if (isQueue) "queue:" else ""}${MessageButtonKt.NEXT}${user.id}",
                emoji = nextEmoji,
                disabled = !nextEnabled
            ),
            secondary(
                id = "${if (isQueue) "queue:" else ""}${MessageButtonKt.END}${user.id}",
                emoji = endEmoji,
                disabled = !endEnabled
            )
        )
}