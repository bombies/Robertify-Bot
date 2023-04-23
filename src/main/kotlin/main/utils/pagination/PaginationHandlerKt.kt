package main.utils.pagination

import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import main.audiohandlers.QueueHandlerKt
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.audio.QueueCommandKt
import main.constants.InteractionLimitsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.api.robertify.imagebuilders.AbstractImageBuilderKt
import main.utils.api.robertify.imagebuilders.ImageBuilderExceptionKt
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilderKt
import main.utils.pagination.pages.DefaultMessagePageKt
import main.utils.pagination.pages.MenuPageKt
import main.utils.pagination.pages.MessagePageKt
import main.utils.pagination.pages.queue.QueueItemKt
import main.utils.pagination.pages.queue.QueuePageKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.FileUpload
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import kotlin.math.ceil

object PaginationHandlerKt {
    private val messages = Collections.synchronizedMap(mutableMapOf<Long, List<MessagePageKt>>())
    private val paginator = PaginationButtonGenerator.DEFAULT
    var embedStyle: () -> EmbedBuilder = { EmbedBuilder() }

    suspend fun paginateMessage(channel: GuildMessageChannel, user: User, messagePages: List<MessagePageKt>) =
        coroutineScope {
            val paginatedMessage = async {
                channel.sendMessageEmbeds(messagePages[0].embed!!)
                    .submit()
                    .thenApply { message ->
                        if (messagePages.size > 1) {
                            message.editMessageComponents(
                                paginator.getButtons(
                                    user = user,
                                    frontEnabled = false,
                                    previousEnabled = false
                                )
                            ).queue()

                            messages[message.idLong] = messagePages
                            return@thenApply message
                        } else return@thenApply null
                    }.join()
            }
            return@coroutineScope paginatedMessage.await()
        }

    suspend fun paginateMessage(event: SlashCommandInteractionEvent, messagePages: List<MessagePageKt>): Message? =
        coroutineScope {
            val paginatedMessage = async {
                var messageAction = event.hook.sendWithEmbed(event.guild) { messagePages[0].embed!! }

                if (messagePages.size > 1)
                    messageAction = messageAction.addComponents(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = false,
                            previousEnabled = false
                        )
                    )

                return@async messageAction.submit()
                    .thenApply { msg ->
                        if (messagePages.size > 1) {
                            messages[msg.idLong] = messagePages
                            return@thenApply msg
                        } else return@thenApply null
                    }.join()
            }
            return@coroutineScope paginatedMessage.await()
        }

    suspend fun paginateQueue(event: SlashCommandInteractionEvent, maxPerPage: Int = 10): Message? {
        val guild = event.guild!!
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val queueHandler = musicManager.scheduler.queueHandler

        event.deferReply().queue()
        val queuePages = messageLogic(guild, queueHandler, maxPerPage)

        return try {
            paginateQueueMessage(event, queuePages)
        } catch (e: ImageBuilderExceptionKt) {
            val defaultMessagePages = queuePages.map { page -> DefaultMessagePageKt(page.embed) }
            paginateMessage(event, defaultMessagePages)
        }
    }

    suspend fun paginateQueueMessage(event: SlashCommandInteractionEvent, queuePages: List<QueuePageKt>) =
        coroutineScope {
            val guild = event.guild!!
            val sendFallbackEmbed: () -> Unit = {
                val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
                val queueHandler = musicManager.scheduler.queueHandler
                val content: List<String> = QueueCommandKt().getContent(guild, queueHandler)

                val pages = messageLogic(event.guild!!, content)
                launch { paginateMessage(event, pages) }
            }

            try {
                val image = queuePages[0].image ?: run {
                    sendFallbackEmbed()
                    return@coroutineScope null
                }

                var messageAction = event.hook
                    .sendFiles(FileUpload.fromData(image, AbstractImageBuilderKt.RANDOM_FILE_NAME))

                if (queuePages.size > 1)
                    messageAction = messageAction.addComponents(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = false,
                            previousEnabled = false
                        )
                    )

                val deferredMsg = async {
                    return@async messageAction.submit()
                        .thenApply { msg ->
                            if (queuePages.size > 1) {
                                messages[msg.idLong] = queuePages
                                return@thenApply msg
                            } else return@thenApply null
                        }.join()
                }

                return@coroutineScope deferredMsg.await()
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException,
                    is ConnectException,
                    is ImageBuilderExceptionKt -> sendFallbackEmbed()

                    else -> throw e
                }
            }

            return@coroutineScope null
        }

    fun paginateMenu(user: User, msg: Message, options: List<StringSelectMenuOptionKt>) {
        val menuPages = menuLogic(msg.id, options)
        val firstPage = menuPages[0]
        val menu = StringSelectMenu(
            customId = "menupage:${user.id}",
            placeholder = "Select an option",
            valueRange = IntRange(1, 1),
            options = firstPage.getOptions().map { it.build() }
        )

        msg.editMessageComponents(ActionRow.of(menu))
            .queue { messages[msg.idLong] = menuPages }
    }

    fun paginateMenu(
        user: User,
        channel: GuildMessageChannel,
        msg: ReplyCallbackAction,
        options: List<StringSelectMenuOptionKt>
    ) {
        val menuPages = menuLogic("null", options)
        val firstPage = menuPages[0]
        val menu = StringSelectMenu(
            customId = "menupage:${user.id}",
            placeholder = "Select an option",
            valueRange = IntRange(1, 1),
            options = firstPage.getOptions().map { it.build() }
        )

        msg.addActionRow(menu)
            .queue {
                it.retrieveOriginal()
                    .queue { original -> messages[original.idLong] = menuPages }
            }
    }

    fun getMessagePages(msgId: Long): List<MessagePageKt>? =
        messages[msgId]

    fun getQueuePages(msgId: Long): List<QueuePageKt>? =
        messages[msgId]?.map { it as QueuePageKt }

    fun getMenuPages(msgId: Long): List<MenuPageKt>? =
        messages[msgId]?.map { it as MenuPageKt }

    fun getSelectionMenu(user: User, options: List<StringSelectMenuOptionKt>): SelectMenu =
        StringSelectionMenuBuilderKt(
            _name = "menuPage:${user.id}",
            placeholder = "Select an option",
            range = Pair(1, 1),
            _options = options.subList(0, options.size.coerceAtMost(InteractionLimitsKt.SELECTION_MENU))
        ).build()

    fun getPaginatedEmbed(
        guild: Guild,
        content: List<*>,
        maxPerPage: Int = 10,
        startingPage: Int = 0,
        numberEachEntry: Boolean = true
    ): MessageEmbed {
        val embedBuilder = embedStyle().appendDescription("\t")
        var index = (startingPage * (if (maxPerPage == 0) 0 else (maxPerPage - 1))) + 1
        for (j in content.indices) {
            if (j == maxPerPage) break

            embedBuilder.appendDescription(
                "${
                    if (numberEachEntry) "**${index++}.** - " else ""
                }${content[j]}\n"
            )
        }
        return embedBuilder.build()
    }

    private fun messageLogic(guild: Guild, content: List<String>, maxPerPage: Int = 10): List<DefaultMessagePageKt> {
        if (content.size < maxPerPage) {
            return listOf(DefaultMessagePageKt(guild, content))
        } else {
            val messagePages = mutableListOf<DefaultMessagePageKt>()
            val pagesRequired = ceil(content.size.toDouble() / maxPerPage.toDouble()).toInt()

            var lastIndex = 0
            for (i in 0 until pagesRequired) {
                val embedBuilder = RobertifyEmbedUtilsKt.embedMessage(guild, "\t")
                for (j in 0 until maxPerPage) {
                    if (lastIndex == content.size) break

                    embedBuilder.appendDescription("${content[lastIndex++]}\n")
                }
                messagePages.add(DefaultMessagePageKt(embedBuilder.build()))
            }
            return messagePages
        }
    }

    private fun messageLogic(guild: Guild, queueHandler: QueueHandlerKt, maxPerPage: Int = 10): List<QueuePageKt> {
        if (queueHandler.size <= maxPerPage) {
            val items = mutableListOf<QueueItemKt>()
            for (i in 0 until queueHandler.size) {
                val track = queueHandler.contents[i]
                items.add(
                    QueueItemKt(
                        trackIndex = i + 1,
                        trackTitle = track.title,
                        artist = track.author,
                        duration = track.length.inWholeMilliseconds
                    )
                )
            }
            return listOf(QueuePageKt(guild, 1, items))
        } else {
            val messagePages = mutableListOf<QueuePageKt>()
            val trackList = queueHandler.contents
            val pagesRequired = ceil(queueHandler.size.toDouble() / maxPerPage.toDouble()).toInt()
            var lastIndex = 0

            for (i in 0 until pagesRequired) {
                val page = QueuePageKt(guild, i + 1)
                for (j in 0 until maxPerPage) {
                    if (lastIndex == queueHandler.size) break
                    val track = trackList[lastIndex++]
                    page.addItem(
                        QueueItemKt(
                            trackIndex = i + 1,
                            trackTitle = track.title,
                            artist = track.author,
                            duration = track.length.inWholeMilliseconds
                        )
                    )
                }
                messagePages.add(page)
            }
            return messagePages
        }
    }

    private fun menuLogic(msgId: String, options: List<StringSelectMenuOptionKt>): List<MenuPageKt> {
        val menuPages = mutableListOf<MenuPageKt>()

        if (options.size <= InteractionLimitsKt.SELECTION_MENU) {
            val page = MenuPageKt()
            options.forEach { page.addOption(it) }
            menuPages.add(page)
        } else {
            val pagesRequired = ceil(options.size.toDouble() / InteractionLimitsKt.SELECTION_MENU.toDouble()).toInt()
            val pageControllers = 1 + ceil(((pagesRequired - 1) * 4) / 2.0).toInt()
            val actualPagesRequired =
                ceil((options.size + pageControllers).toDouble() / InteractionLimitsKt.SELECTION_MENU).toInt()

            var lastIndex = 0
            for (i in 0 until actualPagesRequired) {
                val tempPage = MenuPageKt()
                for (j in 0 until InteractionLimitsKt.SELECTION_MENU) {
                    if (lastIndex == options.size) break

                    if (j == 0 && i != 0) {
                        tempPage.addOption(
                            StringSelectMenuOptionKt(
                                label = "Previous Page",
                                value = "menuPage:previousPage:${msgId}"
                            )
                        )
                        continue
                    }

                    if (j == InteractionLimitsKt.SELECTION_MENU - 1) {
                        tempPage.addOption(
                            StringSelectMenuOptionKt(
                                label = "Next Page",
                                value = "menuPage:nextPage:${msgId}"
                            )
                        )
                        continue
                    }

                    tempPage.addOption(options[lastIndex++])
                }
                menuPages.add(tempPage)
            }
        }
        return menuPages
    }
}