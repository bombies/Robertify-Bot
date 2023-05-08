package main.commands.slashcommands

import main.commands.slashcommands.audio.*
import main.commands.slashcommands.audio.filters.*
import main.commands.slashcommands.dev.*
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
import main.commands.slashcommands.misc.EightBallCommandKt
import main.commands.slashcommands.misc.PingCommandKt
import main.commands.slashcommands.misc.PlaytimeCommandKt
import main.commands.slashcommands.misc.polls.PollCommandKt
import main.commands.slashcommands.misc.reminders.RemindersCommandKt
import main.commands.slashcommands.util.*
import main.commands.slashcommands.util.suggestions.SuggestionCommandKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.modals.ModalMapping
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
        RemoveDuplicatesCommandKt(),
        SearchQueueCommand()
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
        PollCommandKt(),
        RemindersCommandKt(),
        EightBallCommandKt(),
        PingCommandKt(),
        PlaytimeCommandKt()
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
        ManageSuggestionsCommandKt(),
        EvalCommandKt(),
        ShardInfoCommandKt(),
        NodeInfoCommandKt(),
        RandomMessageCommandKt(),
        ReloadConfigCommandKt(),
        ResetPremiumFeaturesCommandKt(),
        SendAlertCommandKt(),
        UpdateCommandKt(),
        PostCommandInfoCommand()
    )

    fun SlashCommandInteractionEvent.getRequiredOption(name: String): OptionMapping =
        this.getOption(name) ?: throw NullPointerException("Invalid option \"$name\". Are you sure that option is required?")

    fun ModalInteractionEvent.getRequiredValue(id: String): ModalMapping =
        this.getValue(id) ?: throw NullPointerException("Invalid value \"$id\". Are you sure that value is required?")

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