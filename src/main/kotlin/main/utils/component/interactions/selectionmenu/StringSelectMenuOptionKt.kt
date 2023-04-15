package main.utils.component.interactions.selectionmenu

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

data class StringSelectMenuOptionKt(
    val label: String,
    val value: String,
    val emoji: Emoji? = null,
    val predicate: ((event: StringSelectInteractionEvent) -> Boolean)? = null
)
