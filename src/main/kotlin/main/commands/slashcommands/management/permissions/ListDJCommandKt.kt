package main.commands.slashcommands.management.permissions

import main.constants.PermissionKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.permissions.PermissionsConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PermissionsMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ListDJCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "listdj",
    description = "List all DJ users and roles in this server.",
    adminOnly = true
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val config = PermissionsConfigKt(guild)
        val localeManager = LocaleManagerKt.getLocaleManager(guild)

        val roles = PermissionsCommandKt().getRolesForPermission(PermissionKt.ROBERTIFY_DJ, config)
        val users = PermissionsCommandKt().getUsersForPermission(PermissionKt.ROBERTIFY_DJ, config)

        event.replyWithEmbed(guild) {
            embedBuilder(
                PermissionsMessages.PERMISSION_LIST,
                Pair("{permission}", PermissionKt.ROBERTIFY_DJ.name)
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