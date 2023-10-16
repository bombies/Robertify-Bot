package main.utils.pagination.pages

import kotlinx.coroutines.runBlocking
import main.utils.RobertifyEmbedUtils
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed

class DefaultMessagePage(private val embed: MessageEmbed?) : MessagePage {

    constructor(guild: Guild?, content: List<String>) : this(
        runBlocking {
            RobertifyEmbedUtils.embedMessage(
                guild,
                "\t${
                    content.reduceIndexed { i, acc, next -> "${acc}${if (i == 1) "\n" else ""}${next}\n" }
                }"
            ).build()
        }
    )

    override fun getEmbed(): MessageEmbed? {
        return embed
    }
}