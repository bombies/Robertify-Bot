package main.utils.pagination.pages

import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import net.dv8tion.jda.api.entities.MessageEmbed

class MenuPage : MessagePage {
    private val options = mutableListOf<StringSelectMenuOption>()

    override suspend fun getEmbed(): MessageEmbed? {
        return null
    }

    fun getOptions(): List<StringSelectMenuOption> =
        options.toList()

    fun addOption(option: StringSelectMenuOption) {
        check(options.size < 25) { "You cannot add anymore options to the page!" }
        options.add(option)
    }

    fun removeOption(index: Int) {
        options.removeAt(index)
    }

    fun getOption(index: Int): StringSelectMenuOption =
        options[index]

    fun toStringList(): List<String> =
        options.map { option ->
            if (!option.toString().contains("Next Page") && !option.toString().contains("Previous Page"))
                return@map option.toString()
            else return@map null
        }.filterNotNull()
}