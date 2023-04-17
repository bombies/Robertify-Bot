package main.commands.contextcommands

import main.utils.component.interactions.contextcommand.AbstractContextCommandKt
import main.utils.internal.delegates.ImmutableListGetDelegate

class ContextCommandManagerKt {
    var commands: List<AbstractContextCommandKt> by ImmutableListGetDelegate()
        private set


    init {
        addCommands()
    }

    private fun addCommands(vararg commands: AbstractContextCommandKt) {
        val newList = this.commands.toMutableList()
        newList.addAll(commands.toList())
        this.commands = newList
    }

    fun getCommand(name: String): AbstractContextCommandKt? =
        commands.find { it.info.name.equals(name, true) }
}