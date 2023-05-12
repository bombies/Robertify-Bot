package main.utils.component.interactions.selectionmenu

import dev.minn.jda.ktx.interactions.components.SelectOption
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption

data class StringSelectMenuOption(
    val label: String,
    val value: String,
    val emoji: Emoji? = null,
    val description: String? = null,
    val predicate: ((event: StringSelectInteractionEvent) -> Boolean)? = null
) {
    fun build(): SelectOption =
        SelectOption(
            label = label,
            value = value,
            emoji = emoji,
            description = description
        )

    override fun toString(): String = label
}
