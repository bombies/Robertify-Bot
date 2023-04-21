package main.utils.pagination.pages

import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import net.dv8tion.jda.api.entities.MessageEmbed

class MenuPageKt : MessagePageKt {
    override val embed: MessageEmbed? = null
    val options = listOf<StringSelectMenuOptionKt>()
}