package main.utils.component.interactions.slashcommand.models

import main.constants.RobertifyPermission
import main.utils.database.mongodb.cache.BotDBCache
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData

data class Command(
    val name: String,
    val description: String,
    val options: List<CommandOption> = listOf(),
    val subCommandGroups: List<SubCommandGroup> = listOf(),
    val subcommands: List<SubCommand> = listOf(),
    val djOnly: Boolean = false,
    val adminOnly: Boolean = false,
    val isPremium: Boolean = false,
    val developerOnly: Boolean = false,
    val botRequiredPermissions: List<Permission> = listOf(),
    val isGuild: Boolean = false,
    val guildUseOnly: Boolean = true,
    private val _checkPermission: ((event: SlashCommandInteractionEvent) -> Boolean)? = null,
    private val _requiredPermissions: List<RobertifyPermission> = listOf(),
) {
    val requiredPermissions = _requiredPermissions
        get() = when {
            adminOnly -> listOf(RobertifyPermission.ROBERTIFY_ADMIN, *field.toTypedArray())
            djOnly -> listOf(RobertifyPermission.ROBERTIFY_DJ, *field.toTypedArray())
            else -> field
        }

    val checkPermission = _checkPermission
        get() = when {
            developerOnly -> { event -> BotDBCache.instance.isDeveloper(event.user.idLong) }
            else -> field
        }

    fun permissionCheck(event: SlashCommandInteractionEvent): Boolean =
        checkPermission?.invoke(event) ?: true

    fun getCommandData(): CommandData {
        val commandData = Commands.slash(name.lowercase(), description)

        // Adding subcommands
        if (subcommands.isNotEmpty() || subCommandGroups.isNotEmpty()) {
            subcommands.forEach { subcommand -> commandData.addSubcommands(subcommand.getSubcommandData()) }
            subCommandGroups.forEach { group -> commandData.addSubcommandGroups(group.build()) }
        } else {
            options.forEach { option ->
                val optionData = OptionData(
                    option.type,
                    option.name.lowercase(),
                    option.description,
                    option.required
                )

                when (option.type) {
                    OptionType.INTEGER -> {
                        if (option.range != null)
                            optionData.setRequiredRange(option.range.first.toLong(), option.range.second.toLong())
                        else {
                            if (option.max != null)
                                optionData.setMaxValue(option.max.toLong())
                            if (option.min != null)
                                optionData.setMinValue(option.min.toLong())
                        }
                        optionData.setAutoComplete(option.autoComplete)
                    }

                    OptionType.NUMBER -> {
                        if (option.range != null)
                            optionData.setRequiredRange(option.range.first.toDouble(), option.range.second.toDouble())
                        else {
                            if (option.max != null)
                                optionData.setMaxValue(option.max.toDouble())
                            if (option.min != null)
                                optionData.setMinValue(option.min.toDouble())
                        }
                        optionData.setAutoComplete(option.autoComplete)
                    }

                    OptionType.STRING -> {
                        if (option.maxLength != null)
                            optionData.setMaxLength(option.maxLength)
                        optionData.setAutoComplete(option.autoComplete)
                    }

                    OptionType.CHANNEL -> {
                        if (option.channelTypes != null)
                            optionData.setChannelTypes(option.channelTypes)
                    }

                    else -> {}
                }
                option.choices.forEach { choice -> optionData.addChoice(choice, choice) }
                commandData.addOptions(optionData)
            }
        }

        commandData.setGuildOnly(guildUseOnly)

        if (developerOnly)
            commandData.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        return commandData
    }
}
