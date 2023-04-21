package main.utils.pagination.pages

import main.utils.RobertifyEmbedUtilsKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed

class DefaultMessagePageKt(override val embed: MessageEmbed) : MessagePageKt {

    constructor(guild: Guild?, content: List<String>) : this(RobertifyEmbedUtilsKt.embedMessage(guild, "\t${
        content.reduceIndexed { i, acc, next -> "${acc}${if (i == 0) "\n" else ""}${next}\n" }
    }").build())
}