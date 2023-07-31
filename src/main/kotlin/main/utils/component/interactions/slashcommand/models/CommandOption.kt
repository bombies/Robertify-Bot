package main.utils.component.interactions.slashcommand.models

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.OptionType

data class CommandOption(
    val type: OptionType = OptionType.STRING,
    val name: String,
    val description: String = "",
    val required: Boolean = true,
    val choices: List<String> = listOf(),
    val maxLength: Int? = null,
    val range: Pair<Int, Int>? = null,
    val max: Int? = null,
    val min: Int? = null,
    val autoComplete: Boolean = false,
    val channelTypes: List<ChannelType>? = null
)
