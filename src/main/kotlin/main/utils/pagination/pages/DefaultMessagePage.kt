package main.utils.pagination.pages

import main.utils.RobertifyEmbedUtils
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed

class DefaultMessagePage(override val embed: MessageEmbed) : MessagePage {

    constructor(guild: Guild?, content: List<String>) : this(RobertifyEmbedUtils.embedMessage(
        guild,
        "\t${
            content.reduceIndexed { i, acc, next -> "${acc}${if (i == 1) "\n" else ""}${next}\n" }
        }"
    ).build())
}