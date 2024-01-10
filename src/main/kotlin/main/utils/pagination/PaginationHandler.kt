package main.utils.pagination

import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import main.audiohandlers.QueueHandler
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.constants.InteractionLimits
import main.utils.GeneralUtils.coerceAtMost
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.api.robertify.imagebuilders.AbstractImageBuilder
import main.utils.api.robertify.imagebuilders.ImageBuilderException
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder
import main.utils.database.mongodb.databases.playlists.PlaylistDB
import main.utils.database.mongodb.databases.playlists.PlaylistModel
import main.utils.database.mongodb.databases.playlists.PlaylistTrack
import main.utils.pagination.pages.AbstractImagePage
import main.utils.pagination.pages.DefaultMessagePage
import main.utils.pagination.pages.MenuPage
import main.utils.pagination.pages.MessagePage
import main.utils.pagination.pages.playlists.PlaylistPage
import main.utils.pagination.pages.playlists.PlaylistsPage
import main.utils.pagination.pages.queue.QueueItem
import main.utils.pagination.pages.queue.QueuePage
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

object PaginationHandler {
    private val logger by SLF4J
    private val messages = Collections.synchronizedMap(mutableMapOf<Long, List<MessagePage>>())
    private val paginator = PaginationButtonGenerator.DEFAULT
    var embedStyle: suspend () -> EmbedBuilder = { EmbedBuilder() }

    suspend fun paginateMessage(channel: GuildMessageChannel, user: User, messagePages: List<MessagePage>) =
        coroutineScope {
            val paginatedMessage = async {
                channel.sendMessageEmbeds(messagePages[0].getEmbed()!!)
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

    suspend fun paginateMessage(
        event: SlashCommandInteractionEvent,
        messagePages: List<MessagePage>,
        isQueue: Boolean = false
    ): Message? =
        coroutineScope {
            val paginatedMessage = async {
                var messageAction = event.hook.sendEmbed(event.guild) { messagePages[0].getEmbed()!! }

                if (messagePages.size > 1)
                    messageAction = messageAction.addComponents(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = false,
                            previousEnabled = false,
                            isQueue = isQueue
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

    suspend fun paginateMessage(
        event: SlashCommandInteractionEvent,
        content: List<String>,
        maxPerPage: Int = 10
    ): Message? {
        event.deferReply().queue()
        val messagePages = messageLogic(event.guild!!, content, maxPerPage)
        return paginateMessage(event, messagePages)
    }

    suspend fun paginatePlaylists(
        event: SlashCommandInteractionEvent,
    ): Message? {
        event.deferReply().queue()

        val guild = event.guild!!
        val user = event.user
        val maxPerPage = 10
        val playlists = PlaylistDB.findPlaylistForUser(user.id).chunked(maxPerPage)
        require(playlists.isNotEmpty()) { "This user has no playlists to display!" }

        val pages = playlists.mapIndexed { i, chunkedPlaylists ->
            PlaylistsPage(
                guild = guild,
                _playlists = chunkedPlaylists,
                pageNumber = i
            )
        }

        return paginateImageMessage(event, pages).await()
    }

    suspend fun paginatePlaylist(
        event: SlashCommandInteractionEvent,
        playlist: PlaylistModel,
        maxTracksPerPage: Int
    ): Message? {
        event.deferReply().queue()

        val guild = event.guild!!
        val trackIndexes = mutableMapOf<PlaylistTrack, Int>()
        playlist.tracks.forEachIndexed { index, playlistTrack -> trackIndexes[playlistTrack] = index }
        val chunkedTracks = playlist.tracks.chunked(maxTracksPerPage)
        val pages = chunkedTracks.mapIndexed { i, tracks ->
            PlaylistPage(
                title = playlist.title,
                artworkUrl = playlist.artwork_url,
                description = playlist.description,
                guild = guild,
                tracks = tracks,
                trackIndexes = trackIndexes,
                pageNumber = i
            )
        }

        return paginateImageMessage(event, pages).await();
    }

    suspend fun paginateQueue(event: SlashCommandInteractionEvent, maxPerPage: Int = 10): Message? {
        val guild = event.guild!!
        val musicManager = RobertifyAudioManager[guild]
        val queueHandler = musicManager.scheduler.queueHandler

        event.deferReply().queue()
        val queuePages = messageLogic(guild, queueHandler, maxPerPage)

        return try {
            paginateQueueMessage(event, queuePages).await()
        } catch (e: ImageBuilderException) {
            val defaultMessagePages = queuePages.map { page -> DefaultMessagePage(page.getEmbed()) }
            paginateMessage(event, defaultMessagePages, true)
        }
    }

    private suspend fun paginateQueueMessage(
        event: SlashCommandInteractionEvent,
        queuePages: List<QueuePage>
    ) = paginateImageMessage(event, queuePages, true);

    private suspend fun paginateImageMessage(
        event: SlashCommandInteractionEvent,
        pages: List<AbstractImagePage>,
        isQueue: Boolean = false,
    ): Deferred<Message?> =
        coroutineScope {
            val guild = event.guild!!
            val sendFallbackEmbed: () -> Deferred<Message?> = {
                async { paginateMessage(event, pages) }
            }

            try {
                val image = pages[0].generateImage() ?: run {
                    return@coroutineScope sendFallbackEmbed()
                }

                var messageAction = event.hook
                    .sendFiles(FileUpload.fromData(image, AbstractImageBuilder.RANDOM_FILE_NAME))

                if (pages.size > 1)
                    messageAction = messageAction.addComponents(
                        paginator.getButtons(
                            user = event.user,
                            frontEnabled = false,
                            previousEnabled = false,
                            isQueue = isQueue
                        )
                    )

                val deferredMsg = async {
                    return@async messageAction.submit()
                        .thenApply { msg ->
                            if (pages.size > 1) {
                                messages[msg.idLong] = pages
                                return@thenApply msg
                            } else return@thenApply null
                        }.join()
                }

                return@coroutineScope deferredMsg
            } catch (e: Exception) {
                return@coroutineScope when (e) {
                    is SocketTimeoutException,
                    is ConnectException,
                    is ImageBuilderException -> sendFallbackEmbed()

                    else -> throw e
                }
            }
        }

    suspend fun paginateMenu(
        event: SlashCommandInteractionEvent,
        options: List<StringSelectMenuOption>,
        startingPage: Int = 0,
        numberEachEntry: Boolean = true
    ) {
        val msg = menuLogic(event, options, startingPage, numberEachEntry)
        paginateMenu(event.user, event.channel.asGuildMessageChannel(), msg, options)
    }

    fun paginateMenu(user: User, msg: Message, options: List<StringSelectMenuOption>) {
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
        options: List<StringSelectMenuOption>
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

    fun getMessagePages(msgId: Long): List<MessagePage>? =
        messages[msgId]

    fun getQueuePages(msgId: Long): List<QueuePage>? =
        messages[msgId]?.map { it as QueuePage }

    fun getMenuPages(msgId: Long): List<MenuPage>? =
        messages[msgId]?.map { it as MenuPage }

    fun getSelectionMenu(user: User, options: List<StringSelectMenuOption>): SelectMenu =
        StringSelectionMenuBuilder(
            _name = "menuPage:${user.id}",
            placeholder = "Select an option",
            range = Pair(1, 1),
            _options = options.subList(0, options.size.coerceAtMost(InteractionLimits.SELECT_MENU_CHOICES))
        ).build()

    suspend fun getPaginatedEmbed(
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

    private suspend fun messageLogic(guild: Guild, content: List<String>, maxPerPage: Int = 10): List<DefaultMessagePage> {
        if (content.size <= maxPerPage) {
            return listOf(DefaultMessagePage(guild, content))
        } else {
            val messagePages = mutableListOf<DefaultMessagePage>()
            val pagesRequired = ceil(content.size.toDouble() / maxPerPage.toDouble()).toInt()
            var lastIndex = 0
            for (i in 0 until pagesRequired) {
                val embedBuilder = RobertifyEmbedUtils.embedMessage(guild, "\t")
                for (j in 0 until maxPerPage) {
                    if (lastIndex == content.size) break

                    embedBuilder.appendDescription("${content[lastIndex++]}\n")
                }
                messagePages.add(DefaultMessagePage(embedBuilder.build()))
            }
            return messagePages
        }
    }

    private fun messageLogic(guild: Guild, queueHandler: QueueHandler, maxPerPage: Int = 10): List<QueuePage> {
        if (queueHandler.size <= maxPerPage) {
            val items = mutableListOf<QueueItem>()
            queueHandler.contents.forEachIndexed { i, track ->
                items.add(
                    QueueItem(
                        trackIndex = i + 1,
                        trackTitle = track.info.title.coerceAtMost(30),
                        artist = track.info.author.coerceAtMost(30),
                        duration = track.length
                    )
                )
            }
            return listOf(QueuePage(guild, 1, items))
        } else {
            val messagePages = mutableListOf<QueuePage>()
            val trackList = queueHandler.contents
            val pagesRequired = ceil(queueHandler.size.toDouble() / maxPerPage.toDouble()).toInt()
            var lastIndex = 0

            for (i in 0 until pagesRequired) {
                val page = QueuePage(guild, i + 1)
                for (j in 0 until maxPerPage) {
                    if (lastIndex == queueHandler.size) break
                    val track = trackList[lastIndex]
                    page.addItem(
                        QueueItem(
                            trackIndex = lastIndex + 1,
                            trackTitle = track.info.title,
                            artist = track.info.author,
                            duration = track.length
                        )
                    )
                    lastIndex++
                }
                messagePages.add(page)
            }
            return messagePages
        }
    }

    private suspend fun menuLogic(
        event: SlashCommandInteractionEvent,
        options: List<StringSelectMenuOption>,
        startingPage: Int,
        numberEachEntry: Boolean
    ): ReplyCallbackAction =
        event.replyEmbeds(getPaginatedEmbed(event.guild!!, options, 25, startingPage, numberEachEntry))

    private fun menuLogic(msgId: String, options: List<StringSelectMenuOption>): List<MenuPage> {
        val menuPages = mutableListOf<MenuPage>()

        if (options.size <= InteractionLimits.SELECT_MENU_CHOICES) {
            val page = MenuPage()
            options.forEach { page.addOption(it) }
            menuPages.add(page)
        } else {
            val pagesRequired = ceil(options.size.toDouble() / InteractionLimits.SELECT_MENU_CHOICES.toDouble()).toInt()
            val pageControllers = 1 + ceil(((pagesRequired - 1) * 4) / 2.0).toInt()
            val actualPagesRequired =
                ceil((options.size + pageControllers).toDouble() / InteractionLimits.SELECT_MENU_CHOICES).toInt()

            var lastIndex = 0
            for (i in 0 until actualPagesRequired) {
                val tempPage = MenuPage()
                for (j in 0 until InteractionLimits.SELECT_MENU_CHOICES) {
                    if (lastIndex == options.size) break

                    if (j == 0 && i != 0) {
                        tempPage.addOption(
                            StringSelectMenuOption(
                                label = "Previous Page",
                                value = "menuPage:previousPage:${msgId}"
                            )
                        )
                        continue
                    }

                    if (j == InteractionLimits.SELECT_MENU_CHOICES - 1) {
                        tempPage.addOption(
                            StringSelectMenuOption(
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