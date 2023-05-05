package main.commands.slashcommands

import main.commands.slashcommands.audio.*
import main.commands.slashcommands.audio.filters.*
import main.commands.slashcommands.dev.ManageSuggestionsCommandKt
import main.commands.slashcommands.management.*
import main.commands.slashcommands.management.bans.BanCommandKt
import main.commands.slashcommands.management.bans.UnbanCommandKt
import main.commands.slashcommands.management.logs.LogCommandKt
import main.commands.slashcommands.management.logs.SetLogChannelCommandKt
import main.commands.slashcommands.management.permissions.ListDJCommandKt
import main.commands.slashcommands.management.permissions.PermissionsCommandKt
import main.commands.slashcommands.management.permissions.RemoveDJCommandKt
import main.commands.slashcommands.management.permissions.SetDJCommandKt
import main.commands.slashcommands.management.requestchannel.RequestChannelCommandKt
import main.commands.slashcommands.management.requestchannel.RequestChannelEditCommandKt
import main.commands.slashcommands.misc.polls.PollCommandKt
import main.commands.slashcommands.util.*
import main.commands.slashcommands.util.suggestions.SuggestionCommandKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.sharding.ShardManager

object SlashCommandManagerKt {

    val musicCommands: List<AbstractSlashCommandKt> = listOf(
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
        VibratoFilterKt(),
        RemoveDuplicatesCommandKt()
    )

    val managementCommands: List<AbstractSlashCommandKt> = listOf(
        RequestChannelEditCommandKt(),
        RequestChannelCommandKt(),
        PermissionsCommandKt(),
        ListDJCommandKt(),
        SetDJCommandKt(),
        RemoveDJCommandKt(),
        BanCommandKt(),
        UnbanCommandKt(),
        LanguageCommandKt(),
        LogCommandKt(),
        SetLogChannelCommandKt(),
        RestrictedChannelsCommandKt(),
        ThemeCommandKt(),
        TogglesCommandKt(),
        TwentyFourSevenCommandKt()
    )

    val miscCommands: List<AbstractSlashCommandKt> = listOf(
        PollCommandKt()
    )

    val utilityCommands: List<AbstractSlashCommandKt> = listOf(
        AlertCommandKt(),
        BotInfoCommandKt(),
        HelpCommandKt(),
        SuggestionCommandKt(),
        SupportServerCommandKt(),
        UptimeCommandKt(),
        VoteCommandKt(),
        WebsiteCommandKt()
    )

    val devCommands: List<AbstractSlashCommandKt> = listOf(
        ManageSuggestionsCommandKt()
    )

    fun SlashCommandInteractionEvent.getRequiredOption(name: String): OptionMapping =
        this.getOption(name) ?: throw NullPointerException("Invalid option \"$name\". Are you sure that option is required!")

    fun ShardManager.registerCommand(command: AbstractSlashCommandKt) =
        command.register(this)

    fun ShardManager.registerCommands(commands: List<AbstractSlashCommandKt>) =
        commands.forEach { it.register(this) }

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