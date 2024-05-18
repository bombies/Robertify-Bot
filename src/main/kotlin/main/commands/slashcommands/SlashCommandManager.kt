package main.commands.slashcommands

import main.commands.slashcommands.audio.*
import main.commands.slashcommands.audio.filters.*
import main.commands.slashcommands.dev.*
import main.commands.slashcommands.dev.test.TestSentryCommand
import main.commands.slashcommands.management.*
import main.commands.slashcommands.management.bans.BanCommand
import main.commands.slashcommands.management.bans.UnbanCommand
import main.commands.slashcommands.management.logs.LogCommand
import main.commands.slashcommands.management.logs.SetLogChannelCommand
import main.commands.slashcommands.management.permissions.ListDJCommand
import main.commands.slashcommands.management.permissions.PermissionsCommand
import main.commands.slashcommands.management.permissions.RemoveDJCommand
import main.commands.slashcommands.management.permissions.SetDJCommand
import main.commands.slashcommands.management.requestchannel.RequestChannelCommand
import main.commands.slashcommands.management.requestchannel.RequestChannelEditCommand
import main.commands.slashcommands.misc.EightBallCommand
import main.commands.slashcommands.misc.PingCommand
import main.commands.slashcommands.misc.PlaytimeCommand
import main.commands.slashcommands.misc.polls.PollCommand
import main.commands.slashcommands.misc.reminders.RemindersCommand
import main.commands.slashcommands.util.*
import main.commands.slashcommands.util.suggestions.SuggestionCommand
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.modals.ModalMapping
import net.dv8tion.jda.api.sharding.ShardManager

object SlashCommandManager {

    val musicCommands: List<AbstractSlashCommand> = listOf(
        PlayCommand(),
        DisconnectCommand(),
        QueueCommand(),
        SkipCommand(),
        NowPlayingCommand(),
        ShuffleCommand(),
        StopCommand(),
        SearchCommand(),
        JumpCommand(),
        JoinCommand(),
        HistoryCommand(),
        FavouriteTracksCommand(),
        ClearQueueCommand(),
        AutoPlayCommand(),
        LoopCommand(),
        LyricsCommand(),
        MoveCommand(),
        PauseCommand(),
        PreviousTrackCommand(),
        RemoveCommand(),
        ResumeCommand(),
        RewindCommand(),
        SeekCommand(),
        VolumeCommand(),
        EightDFilter(),
        KaraokeFilter(),
        NightcoreFilter(),
        TremoloFilter(),
        VibratoFilter(),
        RemoveDuplicatesCommand(),
        SearchQueueCommand()
    )

    val managementCommands: List<AbstractSlashCommand> = listOf(
        RequestChannelEditCommand(),
        RequestChannelCommand(),
        PermissionsCommand(),
        ListDJCommand(),
        SetDJCommand(),
        RemoveDJCommand(),
        BanCommand(),
        UnbanCommand(),
        LanguageCommand(),
        LogCommand(),
        SetLogChannelCommand(),
        RestrictedChannelsCommand(),
        ThemeCommand(),
        TogglesCommand(),
        TwentyFourSevenCommand()
    )

    val miscCommands: List<AbstractSlashCommand> = listOf(
        PollCommand(),
        RemindersCommand(),
        EightBallCommand(),
        PingCommand(),
        PlaytimeCommand()
    )

    val utilityCommands: List<AbstractSlashCommand> = listOf(
        AlertCommand(),
        BotInfoCommand(),
        HelpCommand(),
        SuggestionCommand(),
        SupportServerCommand(),
        UptimeCommand(),
        VoteCommand(),
        WebsiteCommand()
    )

    val devCommands: List<AbstractSlashCommand> = listOf(
        ManageSuggestionsCommand(),
//        EvalCommand(),
        ShardInfoCommand(),
        NodeInfoCommand(),
        RandomMessageCommand(),
        ReloadConfigCommand(),
        ResetPremiumFeaturesCommand(),
        SendAlertCommand(),
        UpdateCommand(),
        PostCommandInfoCommand(),
        TestSentryCommand(),
        CleanupGuildsCommand(),
    )

    fun SlashCommandInteractionEvent.getRequiredOption(name: String): OptionMapping =
        this.getOption(name)
            ?: throw NullPointerException("Invalid option \"$name\". Are you sure that option is required?")

    fun ModalInteractionEvent.getRequiredValue(id: String): ModalMapping =
        this.getValue(id) ?: throw NullPointerException("Invalid value \"$id\". Are you sure that value is required?")

    suspend fun ShardManager.registerCommand(command: AbstractSlashCommand) =
        command.register(this)

    suspend fun ShardManager.registerCommands(commands: List<AbstractSlashCommand>) =
        commands.forEach { it.register(this) }

    val globalCommands: List<AbstractSlashCommand>
        get() {
            val abstractSlashCommands = ArrayList<AbstractSlashCommand>()
            abstractSlashCommands.addAll(musicCommands)
            abstractSlashCommands.addAll(managementCommands)
            abstractSlashCommands.addAll(miscCommands)
            abstractSlashCommands.addAll(utilityCommands)
            return abstractSlashCommands.filter { !it.info.isGuild }
        }

    val guildCommands: List<AbstractSlashCommand>
        get() {
            val abstractSlashCommands = ArrayList<AbstractSlashCommand>()
            abstractSlashCommands.addAll(musicCommands)
            abstractSlashCommands.addAll(managementCommands)
            abstractSlashCommands.addAll(miscCommands)
            abstractSlashCommands.addAll(utilityCommands)
            return abstractSlashCommands.filter { it.info.isGuild }
        }

    fun isMusicCommand(command: AbstractSlashCommand): Boolean =
        musicCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isManagementCommand(command: AbstractSlashCommand): Boolean =
        managementCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isMiscCommand(command: AbstractSlashCommand): Boolean =
        miscCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isUtilityCommand(command: AbstractSlashCommand): Boolean =
        utilityCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isDevCommand(command: AbstractSlashCommand): Boolean =
        devCommands.any { it.info.name.equals(command.info.name, ignoreCase = true) }

    fun isDevCommand(command: String): Boolean =
        devCommands.any { it.info.name.equals(command, ignoreCase = true) }

    fun getCommand(name: String): AbstractSlashCommand? =
        globalCommands.find { it.info.name.equals(name, ignoreCase = true) }

    fun getDevCommand(name: String): AbstractSlashCommand? =
        devCommands.find { it.info.name.equals(name, ignoreCase = true) }

    fun getCommandType(command: AbstractSlashCommand): CommandType? = when {
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