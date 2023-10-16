package main.utils.pagination.events

import dev.minn.jda.ktx.util.SLF4J
import main.constants.MessageButton
import main.events.AbstractEventController
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.locale.messages.GeneralMessages
import main.utils.pagination.PaginationButtonGenerator
import main.utils.pagination.PaginationHandler
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.util.*

class PaginationButtonEvent : AbstractEventController() {
    companion object {
        private val logger by SLF4J
        internal val currentPage = Collections.synchronizedMap(mutableMapOf<Long, Int>())
    }


    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val button = event.button
        val buttonId = button.id ?: return

        if (!buttonId.startsWith(MessageButton.PAGE_ID.toString()))
            return

        event.hook.setEphemeral(true)

        val msg = event.message.idLong
        val paginator = PaginationButtonGenerator.DEFAULT

        if (!currentPage.containsKey(msg))
            currentPage[msg] = 0

        val messagePages = PaginationHandler.getMessagePages(msg)
        if (messagePages == null) {
            event.deferEdit().queue()
            return
        }

        when (buttonId) {
            "${MessageButton.FRONT}${event.user.id}" -> {
                if (currentPage[msg] == 0)
                    return

                currentPage[msg] = 0
                event.editMessageEmbeds(messagePages[currentPage[msg]!!].getEmbed())
                    .setComponents(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = currentPage[msg] != 0,
                            previousEnabled = currentPage[msg] != 0,
                        )
                    )
                    .queue()
            }

            "${MessageButton.PREVIOUS}${event.user.id}" -> {
                currentPage[msg] = currentPage[msg]!!.minus(1)
                event.editMessageEmbeds(messagePages[currentPage[msg]!!].getEmbed())
                    .setComponents(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = currentPage[msg] != 0,
                            previousEnabled = currentPage[msg] != 0,
                        )
                    )
                    .queue()
            }

            "${MessageButton.NEXT}${event.user.id}" -> {
                currentPage[msg] = currentPage[msg]!!.plus(1)
                event.editMessageEmbeds(messagePages[currentPage[msg]!!].getEmbed())
                    .setComponents(
                        paginator.getButtons(
                            user = event.user,
                            nextEnabled = currentPage[msg] != messagePages.size - 1,
                            endEnabled = currentPage[msg] != messagePages.size - 1,
                        )
                    )
                    .queue()
            }

            "${MessageButton.END}${event.user.id}" -> {
                if (currentPage[msg] == messagePages.size - 1)
                    return

                currentPage[msg] = messagePages.size - 1
                event.editMessageEmbeds(messagePages[currentPage[msg]!!].getEmbed())
                    .setComponents(
                        paginator.getButtons(
                            user = event.user,
                            nextEnabled = currentPage[msg] != messagePages.size - 1,
                            endEnabled = currentPage[msg] != messagePages.size - 1,
                        )
                    )
                    .queue()
            }

            else -> event.replyEmbed {
                embed(GeneralMessages.NO_PERMS_BUTTON)
            }.setEphemeral(true).queue()
        }
    }
}