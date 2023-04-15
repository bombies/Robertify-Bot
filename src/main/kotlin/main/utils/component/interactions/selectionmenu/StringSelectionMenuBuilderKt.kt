package main.utils.component.interactions.selectionmenu

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

data class StringSelectionMenuBuilderKt(
    private val _name: String,
    private val _options: List<StringSelectMenuOptionKt>,
    val placeholder: String,
    val range: Pair<Int, Int>,
    val permissionCheck: ((event: StringSelectInteractionEvent) -> Boolean)? = null,
    val limitedTo: Long? = null
) {
    val name = _name
        get() = if (limitedTo != null)
            "$field:$limitedTo"
        else field

    val options = _options
        get() = field.subList(0, field.size.coerceAtMost(25))

    fun build(): StringSelectMenu {
        val builder = StringSelectMenu.create(name)
            .setPlaceholder(placeholder)
            .setRequiredRange(range.first, range.second)

        options.forEach { option ->
            builder.addOption(option.label, option.value, null, option.emoji)
        }

        return builder.build()
    }
}
