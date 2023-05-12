package main.utils.component.interactions.slashcommand.models

import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData

data class SubCommandGroup(
    private val name: String,
    private val description: String,
    private val subCommands: List<SubCommand>
) {

    fun build(): SubcommandGroupData {
        val data = SubcommandGroupData(name.lowercase(), description)
        subCommands.forEach { command ->
            val subcommandData = SubcommandData(command.name.lowercase(), command.description)

            command.options.forEach { option ->
                val optionData = OptionData(
                    option.type,
                    option.name.lowercase(),
                    option.description,
                    option.required,
                    option.autoComplete
                )

                option.choices.forEach { choice -> optionData.addChoice(choice, choice) }
                subcommandData.addOptions(optionData)
            }

            data.addSubcommands(subcommandData)
        }

        return data
    }
}
