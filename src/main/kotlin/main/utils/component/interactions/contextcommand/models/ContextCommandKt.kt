package main.utils.component.interactions.contextcommand.models

import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands

data class ContextCommandKt(val type: Command.Type, val name: String) {
    fun getCommandData(): CommandData =
        Commands.context(type, name)
}
