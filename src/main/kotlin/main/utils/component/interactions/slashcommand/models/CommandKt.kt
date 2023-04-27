package main.utils.component.interactions.slashcommand.models

import main.constants.PermissionKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData

data class CommandKt(
    val name: String,
    val description: String,
    val options: List<CommandOptionKt> = listOf(),
    val subCommandGroups: List<SubCommandGroupKt> = listOf(),
    val subcommands: List<SubCommandKt> = listOf(),
    val djOnly: Boolean = false,
    val adminOnly: Boolean = false,
    val isPremium: Boolean = false,
    val isPrivate: Boolean = false,
    val botRequiredPermissions: List<Permission> = listOf(),
    val isGuild: Boolean = false,
    val guildUseOnly: Boolean = true,
    private val _checkPermission: ((event: SlashCommandInteractionEvent) -> Boolean)? = null,
    private val _requiredPermissions: List<PermissionKt> = listOf(),
) {
    val requiredPermissions = _requiredPermissions
        get() = when {
            adminOnly -> listOf(PermissionKt.ROBERTIFY_ADMIN, *field.toTypedArray())
            djOnly -> listOf(PermissionKt.ROBERTIFY_DJ, *field.toTypedArray())
            else -> field
        }

    val checkPermission = _checkPermission
        get() = when {
            isPrivate -> { event -> BotDBCacheKt.instance.isDeveloper(event.user.idLong) }
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
                    }

                    OptionType.STRING -> {
                        if (option.maxLength != null)
                            optionData.setMaxLength(option.maxLength)
                    }

                    OptionType.CHANNEL -> {
                        if (option.channelTypes != null)
                            optionData.setChannelTypes(option.channelTypes)
                    }

                    else -> {}
                }

                if (option.choices.isNotEmpty())
                    optionData.setAutoComplete(option.autoComplete)

                option.choices.forEach { choice -> optionData.addChoice(choice, choice) }
                commandData.addOptions(optionData)
            }
        }

        commandData.setGuildOnly(guildUseOnly)
        return commandData
    }
}
