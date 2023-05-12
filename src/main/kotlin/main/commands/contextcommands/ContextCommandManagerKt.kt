package main.commands.contextcommands

import main.utils.component.interactions.contextcommand.AbstractContextCommand
import main.utils.internal.delegates.ImmutableListGetDelegate

class ContextCommandManagerKt {
    var commands: List<AbstractContextCommand> by ImmutableListGetDelegate()
        private set


    init {
        addCommands()
    }

    private fun addCommands(vararg commands: AbstractContextCommand) {
        val newList = this.commands.toMutableList()
        newList.addAll(commands.toList())
        this.commands = newList
    }

    fun getCommand(name: String): AbstractContextCommand? =
        commands.find { it.info.name.equals(name, true) }
}