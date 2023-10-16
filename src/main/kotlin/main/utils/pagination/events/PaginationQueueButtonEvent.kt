package main.utils.pagination.events

import dev.minn.jda.ktx.messages.editMessage_
import main.constants.MessageButton
import main.events.AbstractEventController
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageBuilderException
import main.utils.locale.messages.GeneralMessages
import main.utils.pagination.PaginationButtonGenerator
import main.utils.pagination.PaginationHandler
import main.utils.pagination.pages.queue.QueuePage
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.utils.AttachedFile
import java.net.ConnectException
import java.net.SocketTimeoutException

class PaginationQueueButtonEvent : AbstractEventController() {

    companion object {
        val currentPage: MutableMap<Long, Int> = PaginationButtonEvent.currentPage
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val button = event.button
        val buttonId = button.id ?: return
        if (!buttonId.startsWith("queue:${MessageButton.PAGE_ID}"))
            return

        event.hook.setEphemeral(true)
        val msg = event.message.idLong
        val paginator = PaginationButtonGenerator.DEFAULT

        if (!currentPage.containsKey(msg))
            currentPage[msg] = 0

        val queuePages = PaginationHandler.getQueuePages(msg)

        if (queuePages == null) {
            event.deferEdit().queue()
            return
        }

        fun handleButtonPress(
            queuePage: QueuePage,
            frontEnabled: Boolean = true,
            previousEnabled: Boolean = true,
            nextEnabled: Boolean = true,
            endEnabled: Boolean = true
        ) {
            val sendEmbed: () -> Unit = {
                event.editMessageEmbeds(queuePage.getEmbed())
                    .setComponents(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = currentPage[msg] != 0,
                            previousEnabled = currentPage[msg] != 0,
                            isQueue = true
                        )
                    ).queue()
            }

            try {
                val image = queuePage.generateImage() ?: run {
                    sendEmbed()
                    return
                }

                event.editMessage_(
                    attachments = listOf(
                        AttachedFile.fromData(
                            image,
                            AbstractImageBuilder.RANDOM_FILE_NAME
                        )
                    ),

                    components = listOf(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = frontEnabled,
                            previousEnabled = previousEnabled,
                            nextEnabled = nextEnabled,
                            endEnabled = endEnabled,
                            isQueue = true
                        )
                    )
                ).queue()
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException,
                    is ConnectException,
                    is ImageBuilderException -> {
                        sendEmbed()
                    }

                    else -> throw e
                }
            }
        }

        when (buttonId) {
            "queue:${MessageButton.FRONT}${event.user.id}" -> {
                if (currentPage[msg] == 0)
                    return
                currentPage[msg] = 0
                val queuePage = queuePages[0]
                handleButtonPress(
                    queuePage = queuePage,
                    frontEnabled = currentPage[msg] != 0,
                    previousEnabled = currentPage[msg] != 0
                )
            }

            "queue:${MessageButton.PREVIOUS}${event.user.id}" -> {
                currentPage[msg] = currentPage[msg]!!.minus(1)
                val queuePage = queuePages[currentPage[msg]!!]
                handleButtonPress(
                    queuePage = queuePage,
                    frontEnabled = currentPage[msg] != 0,
                    previousEnabled = currentPage[msg] != 0
                )
            }

            "queue:${MessageButton.NEXT}${event.user.id}" -> {
                currentPage[msg] = currentPage[msg]!!.plus(1)
                val queuePage = queuePages[currentPage[msg]!!]
                handleButtonPress(
                    queuePage = queuePage,
                    nextEnabled = currentPage[msg] != queuePages.size - 1,
                    endEnabled = currentPage[msg] != queuePages.size - 1
                )
            }

            "queue:${MessageButton.END}${event.user.id}" -> {
                if (currentPage[msg] == queuePages.size - 1)
                    return
                currentPage[msg] = queuePages.size - 1
                val queuePage = queuePages[queuePages.size - 1]
                handleButtonPress(
                    queuePage = queuePage,
                    nextEnabled = currentPage[msg] != queuePages.size - 1,
                    endEnabled = currentPage[msg] != queuePages.size - 1
                )
            }

            else -> event.replyEmbed {
                embed(GeneralMessages.NO_PERMS_BUTTON)
            }.setEphemeral(true).queue()
        }
    }
}