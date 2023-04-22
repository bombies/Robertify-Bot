package main.utils.component.interactions.slashcommand.models

import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

data class SubCommandKt(
    val name: String,
    val description: String,
    val options: List<CommandOptionKt> = emptyList()
) {

    fun getSubcommandData(): SubcommandData {
        val subcommandData = SubcommandData(name, description)

        options.forEach { option ->
            val optionData = OptionData(
                option.type,
                option.name,
                option.description,
                option.required
            )

            option.choices.forEach { choice -> optionData.addChoice(choice, choice) }
            subcommandData.addOptions(optionData)
        }

        return subcommandData
    }
}
