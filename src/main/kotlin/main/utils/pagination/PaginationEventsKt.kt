package main.utils.pagination

import dev.minn.jda.ktx.util.SLF4J
import main.constants.MessageButtonKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.api.robertify.imagebuilders.AbstractImageBuilderKt
import main.utils.api.robertify.imagebuilders.ImageBuilderExceptionKt
import main.utils.events.AbstractEventControllerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import main.utils.pagination.pages.queue.QueuePageKt
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.utils.AttachedFile
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*

class PaginationEventsKt : AbstractEventControllerKt() {

    companion object {
        private val logger by SLF4J
        private val currentPage = Collections.synchronizedMap(mutableMapOf<Long, Int>())
    }

    override fun eventHandlerInvokers() {
        onRegularButtonClick()
        onQueueButtonClick()
    }

    private fun onRegularButtonClick() =
        onEvent<ButtonInteractionEvent> { event ->
            val button = event.button
            val buttonId = button.id ?: return@onEvent
            if (!buttonId.startsWith(MessageButtonKt.PAGE_ID.toString()))
                return@onEvent

            event.hook.setEphemeral(true)

            val msg = event.message.idLong
            val paginator = PaginationButtonGenerator.DEFAULT

            if (!currentPage.containsKey(msg))
                currentPage[msg] = 0

            val messagePages = PaginationHandlerKt.getMessagePages(msg)
            if (messagePages == null) {
                event.deferEdit().queue()
                return@onEvent
            }

            when (buttonId) {
                "${MessageButtonKt.FRONT}${event.user.id}" -> {
                    if (currentPage[msg] == 0)
                        return@onEvent

                    currentPage[msg] = 0
                    event.editMessageEmbeds(messagePages[0].embed)
                        .setComponents(
                            paginator.getButtons(
                                user = event.user,
                                frontEnabled = currentPage[msg] != 0,
                                previousEnabled = currentPage[msg] != 0,
                            )
                        )
                }

                "${MessageButtonKt.PREVIOUS}${event.user.id}" -> {
                    currentPage[msg] = currentPage[msg]!!.minus(1)
                    event.editMessageEmbeds(messagePages[0].embed)
                        .setComponents(
                            paginator.getButtons(
                                user = event.user,
                                frontEnabled = currentPage[msg] != 0,
                                previousEnabled = currentPage[msg] != 0,
                            )
                        )
                }

                "${MessageButtonKt.NEXT}${event.user.id}" -> {
                    currentPage[msg] = currentPage[msg]!!.plus(1)
                    event.editMessageEmbeds(messagePages[0].embed)
                        .setComponents(
                            paginator.getButtons(
                                user = event.user,
                                nextEnabled = currentPage[msg] == messagePages.size - 1,
                                endEnabled = currentPage[msg] == messagePages.size - 1,
                            )
                        )
                }

                "${MessageButtonKt.END}${event.user.id}" -> {
                    if (currentPage[msg] == messagePages.size - 1)
                        return@onEvent

                    currentPage[msg] = messagePages.size - 1
                    event.editMessageEmbeds(messagePages[0].embed)
                        .setComponents(
                            paginator.getButtons(
                                user = event.user,
                                nextEnabled = currentPage[msg] == messagePages.size - 1,
                                endEnabled = currentPage[msg] == messagePages.size - 1,
                            )
                        )
                }

                else -> event.replyWithEmbed(event.guild) {
                    embed(RobertifyLocaleMessageKt.GeneralMessages.NO_PERMS_BUTTON)
                }.setEphemeral(true).queue()
            }
        }

    private fun onQueueButtonClick() =
        onEvent<ButtonInteractionEvent> { event ->
            val button = event.button
            val buttonId = button.id ?: return@onEvent
            if (!buttonId.startsWith("queue:${MessageButtonKt.PAGE_ID}"))
                return@onEvent

            event.hook.setEphemeral(true)
            val msg = event.message.idLong
            val paginator = PaginationButtonGenerator.DEFAULT

            if (!currentPage.containsKey(msg))
                currentPage[msg] = 0

            val queuePages = PaginationHandlerKt.getQueuePages(msg)

            if (queuePages == null) {
                event.deferEdit().queue()
                return@onEvent
            }

            fun handleButtonPress(
                queuePage: QueuePageKt,
                frontEnabled: Boolean = true,
                previousEnabled: Boolean = true,
                nextEnabled: Boolean = true,
                endEnabled: Boolean = true
            ) {
                val sendEmbed: () -> Unit = {
                    event.editMessageEmbeds(queuePage.embed)
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
                    val image = queuePage.image ?: run {
                        sendEmbed()
                        return
                    }

                    event.editMessageAttachments(
                        AttachedFile.fromData(
                            image,
                            AbstractImageBuilderKt.RANDOM_FILE_NAME
                        )
                    )
                        .setComponents(
                            paginator.getButtons(
                                user = event.user,
                                frontEnabled = frontEnabled,
                                previousEnabled = previousEnabled,
                                nextEnabled = nextEnabled,
                                endEnabled = endEnabled,
                                isQueue = true
                            )
                        ).queue()
                } catch (e: Exception) {
                    when (e) {
                        is SocketTimeoutException,
                        is ConnectException,
                        is ImageBuilderExceptionKt -> {
                            sendEmbed()
                        }

                        else -> throw e
                    }
                }
            }

            when (buttonId) {
                "queue:${MessageButtonKt.FRONT}${event.user.id}" -> {
                    if (currentPage[msg] == 0)
                        return@onEvent
                    currentPage[msg] = 0
                    val queuePage = queuePages[0]
                    handleButtonPress(
                        queuePage = queuePage,
                        frontEnabled = currentPage[msg] != 0,
                        previousEnabled = currentPage[msg] != 0
                    )
                }

                "queue:${MessageButtonKt.PREVIOUS}${event.user.id}" -> {
                    currentPage[msg] = currentPage[msg]!!.minus(1)
                    val queuePage = queuePages[currentPage[msg]!!]
                    handleButtonPress(
                        queuePage = queuePage,
                        frontEnabled = currentPage[msg] != 0,
                        previousEnabled = currentPage[msg] != 0
                    )
                }

                "queue:${MessageButtonKt.NEXT}${event.user.id}" -> {
                    currentPage[msg] = currentPage[msg]!!.plus(1)
                    val queuePage = queuePages[currentPage[msg]!!]
                    handleButtonPress(
                        queuePage = queuePage,
                        nextEnabled = currentPage[msg] != queuePages.size - 1,
                        endEnabled = currentPage[msg] != queuePages.size - 1
                    )
                }

                "queue:${MessageButtonKt.END}${event.user.id}" -> {
                    if (currentPage[msg] == queuePages.size - 1)
                        return@onEvent
                    currentPage[msg] = queuePages.size - 1
                    val queuePage = queuePages[queuePages.size - 1]
                    handleButtonPress(
                        queuePage = queuePage,
                        nextEnabled = currentPage[msg] != queuePages.size - 1,
                        endEnabled = currentPage[msg] != queuePages.size - 1
                    )
                }

                else -> event.replyWithEmbed(event.guild) {
                    embed(RobertifyLocaleMessageKt.GeneralMessages.NO_PERMS_BUTTON)
                }.setEphemeral(true).queue()
            }
        }


}