package main.utils.component.interactions.slashcommand.models

import net.dv8tion.jda.api.interactions.commands.OptionType

data class CommandOptionKt(
    val type: OptionType = OptionType.STRING,
    val name: String,
    val description: String = "",
    val required: Boolean = true,
    val choices: List<String> = listOf()
)
