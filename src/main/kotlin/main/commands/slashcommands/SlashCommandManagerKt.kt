package main.commands.slashcommands

import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.internal.delegates.ImmutableListGetDelegate

class SlashCommandManagerKt private constructor() {
    companion object {
        val ins = SlashCommandManagerKt()
    }

    var musicCommands: List<AbstractSlashCommandKt> by ImmutableListGetDelegate()
        private set
    var managementCommands: List<AbstractSlashCommandKt> by ImmutableListGetDelegate()
        private set
    var miscCommands: List<AbstractSlashCommandKt> by ImmutableListGetDelegate()
        private set
    var utilityCommands: List<AbstractSlashCommandKt> by ImmutableListGetDelegate()
        private set
    var devCommands: List<AbstractSlashCommandKt> by ImmutableListGetDelegate()
        private set

    val globalCommands: List<AbstractSlashCommandKt>
        get() {
            val abstractSlashCommands = ArrayList<AbstractSlashCommandKt>()
            abstractSlashCommands.addAll(musicCommands)
            abstractSlashCommands.addAll(managementCommands)
            abstractSlashCommands.addAll(miscCommands)
            abstractSlashCommands.addAll(utilityCommands)
            return abstractSlashCommands.filter { !it.info.isGuild }
        }

    val guildCommands: List<AbstractSlashCommandKt>
        get() {
            val abstractSlashCommands = ArrayList<AbstractSlashCommandKt>()
            abstractSlashCommands.addAll(musicCommands)
            abstractSlashCommands.addAll(managementCommands)
            abstractSlashCommands.addAll(miscCommands)
            abstractSlashCommands.addAll(utilityCommands)
            return abstractSlashCommands.filter { it.info.isGuild }
        }

    init {
        addMusicCommands()
        addManagementCommands()
        addMiscCommands()
        addUtilityCommands()
        addDevCommands()
    }

    private fun addMusicCommands(vararg commands: AbstractSlashCommandKt) {
        val newList = musicCommands.toMutableList()
        newList.addAll(commands.toList())
        musicCommands = newList
    }

    private fun addManagementCommands(vararg commands: AbstractSlashCommandKt) {
        val newList = managementCommands.toMutableList()
        newList.addAll(commands.toList())
        managementCommands = newList
    }

    private fun addMiscCommands(vararg commands: AbstractSlashCommandKt) {
        val newList = miscCommands.toMutableList()
        newList.addAll(commands.toList())
        miscCommands = newList
    }

    private fun addUtilityCommands(vararg commands: AbstractSlashCommandKt) {
        val newList = utilityCommands.toMutableList()
        newList.addAll(commands.toList())
        utilityCommands = newList
    }

    private fun addDevCommands(vararg commands: AbstractSlashCommandKt) {
        val newList = devCommands.toMutableList()
        newList.addAll(commands.toList())
        devCommands = newList
    }

    fun isMusicCommand(command: AbstractSlashCommandKt): Boolean =
        musicCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isManagementCommand(command: AbstractSlashCommandKt): Boolean =
        managementCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isMiscCommand(command: AbstractSlashCommandKt): Boolean =
        miscCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isUtilityCommand(command: AbstractSlashCommandKt): Boolean =
        utilityCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isDevCommand(command: AbstractSlashCommandKt): Boolean =
        devCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isDevCommand(command: String): Boolean =
        devCommands.any { it.info.name.equals(command, ignoreCase = true) }

    fun getCommand(name: String): AbstractSlashCommandKt? =
        globalCommands.find { it.info.name.equals(name, ignoreCase = true) }

    fun getDevCommand(name: String): AbstractSlashCommandKt? =
        devCommands.find { it.info.name.equals(name, ignoreCase = true) }

    fun getCommandType(command: AbstractSlashCommandKt): CommandType? = when {
        isMusicCommand(command) -> CommandType.MUSIC
        isManagementCommand(command) -> CommandType.MANAGEMENT
        isMiscCommand(command) -> CommandType.MISCELLANEOUS
        isUtilityCommand(command) -> CommandType.UTILITY
        else -> null
    }

    enum class CommandType {
        MUSIC,
        MANAGEMENT,
        MISCELLANEOUS,
        UTILITY
    }
}