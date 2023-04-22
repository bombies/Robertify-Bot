package main.utils.pagination.pages

import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import net.dv8tion.jda.api.entities.MessageEmbed

class MenuPageKt : MessagePageKt {
    override val embed: MessageEmbed? = null
    private val options = mutableListOf<StringSelectMenuOptionKt>()

    fun getOptions(): List<StringSelectMenuOptionKt> =
        options.toList()

    fun addOption(option: StringSelectMenuOptionKt) {
        check(options.size < 25) { "You cannot add anymore options to the page!" }
        options.add(option)
    }

    fun removeOption(index: Int) {
        options.removeAt(index)
    }

    fun getOption(index: Int): StringSelectMenuOptionKt =
        options[index]

    fun toStringList(): List<String> =
        options.map { option ->
            if (!option.toString().contains("Next Page") && !option.toString().contains("Previous Page"))
                return@map option.toString()
            else return@map null
        }.filterNotNull()
}