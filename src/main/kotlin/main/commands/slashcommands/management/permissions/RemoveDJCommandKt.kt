package main.commands.slashcommands.management.permissions

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.PermissionKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.permissions.PermissionsConfigKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class RemoveDJCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "removedj",
    description = "Remove a user or role as a DJ in this server.",
    options = listOf(
        CommandOptionKt(
            type = OptionType.MENTIONABLE,
            name = "target",
            description = "The user or role to remove as a DJ."
        )
    ),
    adminOnly = true
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val mentionable = event.getRequiredOption("target").asMentionable
        event.replyWithEmbed { handleRemoveDJ(event.guild!!, mentionable) }
            .queue()
    }

    private fun handleRemoveDJ(guild: Guild, mentionable: IMentionable): MessageEmbed {
        val config = PermissionsConfigKt(guild)

        return when {
            mentionable.asMention.startsWith("<@&") -> {
                try {
                    config.removeRoleFromPermission(mentionable.idLong, PermissionKt.ROBERTIFY_DJ)
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        RobertifyLocaleMessageKt.PermissionsMessages.DJ_REMOVED,
                        Pair("{mentionable}", mentionable.asMention)
                    ).build()
                } catch (e: IllegalAccessException) {
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        RobertifyLocaleMessageKt.PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                        Pair("{mentionable}", mentionable.asMention),
                        Pair("{permission}", PermissionKt.ROBERTIFY_DJ.name)
                    ).build()
                }
            }

            mentionable.asMention.startsWith("<@") -> {
                if (!config.userHasPermission(mentionable.idLong, PermissionKt.ROBERTIFY_DJ))
                    return RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        RobertifyLocaleMessageKt.PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                        Pair("{mentionable}", mentionable.asMention),
                        Pair("{permission}", PermissionKt.ROBERTIFY_DJ.name)
                    ).build()

                config.removePermissionFromUser(mentionable.idLong, PermissionKt.ROBERTIFY_DJ)
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    RobertifyLocaleMessageKt.PermissionsMessages.DJ_REMOVED,
                    Pair("{mentionable}", mentionable.asMention)
                ).build()
            }

            else -> RobertifyEmbedUtilsKt
                .embedMessage(guild, RobertifyLocaleMessageKt.GeneralMessages.INVALID_ARGS)
                .build()
        }
    }
}