package main.commands.slashcommands.management.permissions

import main.constants.RobertifyPermission
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.permissions.PermissionsConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PermissionsMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ListDJCommand : AbstractSlashCommand(SlashCommand(
    name = "listdj",
    description = "List all DJ users and roles in this server.",
    adminOnly = true
)) {

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val config = PermissionsConfig(guild)
        val localeManager = LocaleManager[guild]

        val roles = PermissionsCommand().getRolesForPermission(RobertifyPermission.ROBERTIFY_DJ, config)
        val users = PermissionsCommand().getUsersForPermission(RobertifyPermission.ROBERTIFY_DJ, config)

        event.replyEmbed {
            embedBuilder(
                PermissionsMessages.PERMISSION_LIST,
                Pair("{permission}", RobertifyPermission.ROBERTIFY_DJ.name)
            )
                .addField(
                    localeManager.getMessage(PermissionsMessages.PERMISSIONS_ROLES),
                    if (roles.isEmpty()) localeManager.getMessage(GeneralMessages.NOTHING_HERE) else roles.joinToString(", "),
                    false
                )
                .addField(
                    localeManager.getMessage(PermissionsMessages.PERMISSIONS_USERS),
                    if (users.isEmpty()) localeManager.getMessage(GeneralMessages.NOTHING_HERE) else users.joinToString(", "),
                    false
                )
                .build()
        }.queue()
    }
}