package main.commands.slashcommands.management.permissions

import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.RobertifyPermission
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.permissions.PermissionsConfig
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PermissionsMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class RemoveDJCommand : AbstractSlashCommand(SlashCommand(
    name = "removedj",
    description = "Remove a user or role as a DJ in this server.",
    options = listOf(
        CommandOption(
            type = OptionType.MENTIONABLE,
            name = "target",
            description = "The user or role to remove as a DJ."
        )
    ),
    adminOnly = true
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val mentionable = event.getRequiredOption("target").asMentionable
        event.replyEmbed { handleRemoveDJ(event.guild!!, mentionable) }
            .queue()
    }

    private suspend fun handleRemoveDJ(guild: Guild, mentionable: IMentionable): MessageEmbed {
        val config = PermissionsConfig(guild)

        return when {
            mentionable.asMention.startsWith("<@&") -> {
                try {
                    config.removeRoleFromPermission(mentionable.idLong, RobertifyPermission.ROBERTIFY_DJ)
                    RobertifyEmbedUtils.embedMessage(
                        guild,
                        PermissionsMessages.DJ_REMOVED,
                        Pair("{mentionable}", mentionable.asMention)
                    ).build()
                } catch (e: IllegalAccessException) {
                    RobertifyEmbedUtils.embedMessage(
                        guild,
                        PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                        Pair("{mentionable}", mentionable.asMention),
                        Pair("{permission}", RobertifyPermission.ROBERTIFY_DJ.name)
                    ).build()
                }
            }

            mentionable.asMention.startsWith("<@") -> {
                if (!config.userHasPermission(mentionable.idLong, RobertifyPermission.ROBERTIFY_DJ))
                    return RobertifyEmbedUtils.embedMessage(
                        guild,
                        PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                        Pair("{mentionable}", mentionable.asMention),
                        Pair("{permission}", RobertifyPermission.ROBERTIFY_DJ.name)
                    ).build()

                config.removePermissionFromUser(mentionable.idLong, RobertifyPermission.ROBERTIFY_DJ)
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    PermissionsMessages.DJ_REMOVED,
                    Pair("{mentionable}", mentionable.asMention)
                ).build()
            }

            else -> RobertifyEmbedUtils
                .embedMessage(guild, GeneralMessages.INVALID_ARGS)
                .build()
        }
    }
}