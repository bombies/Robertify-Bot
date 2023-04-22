package main.utils.pagination

import main.utils.pagination.pages.MessagePageKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.Collections

object PaginationHandlerKt {
    private val messages = Collections.synchronizedMap(mutableMapOf<Long, List<MessagePageKt>>())
    private var embedStyle: () -> EmbedBuilder = { EmbedBuilder() }

    fun paginateMessage(channel: GuildMessageChannel, user: User, messagePages: List<MessagePageKt>) {

    }
}