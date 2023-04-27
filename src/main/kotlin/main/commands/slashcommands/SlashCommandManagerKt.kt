package main.commands.slashcommands

import main.commands.slashcommands.audio.*
import main.commands.slashcommands.audio.filters.*
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.internal.delegates.ImmutableListGetDelegate
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.sharding.ShardManager
import java.lang.NullPointerException

object SlashCommandManagerKt {

    fun SlashCommandInteractionEvent.getRequiredOption(name: String): OptionMapping =
        this.getOption(name) ?: throw NullPointerException("Invalid option \"$name\". Are you sure that option is required!")

    fun ShardManager.registerCommand(command: AbstractSlashCommandKt) =
        command.register(this)

    fun ShardManager.registerCommands(commands: List<AbstractSlashCommandKt>) =
        commands.forEach { it.register(this) }

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
        addMusicCommands(
            PlayCommandKt(),
            DisconnectCommandKt(),
            QueueCommandKt(),
            SkipCommandKt(),
            NowPlayingCommandKt(),
            ShuffleCommandKt(),
            StopCommandKt(),
            SearchCommandKt(),
            JumpCommandKt(),
            JoinCommandKt(),
            HistoryCommandKt(),
            FavouriteTracksCommandKt(),
            ClearQueueCommandKt(),
            AutoPlayCommandKt(),
            LoopCommandKt(),
            LyricsCommandKt(),
            MoveCommandKt(),
            PauseCommandKt(),
            PreviousTrackCommandKt(),
            RemoveCommandKt(),
            ResumeCommandKt(),
            RewindCommandKt(),
            SeekCommandKt(),
            VolumeCommandKt(),
            EightDFilterKt(),
            KaraokeFilterKt(),
            NightcoreFilterKt(),
            TremoloFilterKt(),
            VibratoFilterKt()
        )
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