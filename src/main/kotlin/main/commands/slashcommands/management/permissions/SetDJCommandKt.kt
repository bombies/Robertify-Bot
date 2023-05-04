package main.commands.slashcommands.management.permissions

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.RobertifyPermissionKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.permissions.PermissionsConfigKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PermissionsMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class SetDJCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "setdj",
        description = "Set a user or role to be a DJ in this server.",
        options = listOf(
            CommandOptionKt(
                type = OptionType.MENTIONABLE,
                name = "target",
                description = "The user or role to set as a DJ."
            )
        ),
        adminOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val mentionable = event.getRequiredOption("target").asMentionable
        event.replyWithEmbed { handleSetDJ(event.guild!!, mentionable) }
            .queue()
    }

    private fun handleSetDJ(guild: Guild, mentionable: IMentionable): MessageEmbed {
        val config = PermissionsConfigKt(guild)

        return when {
            mentionable.asMention.startsWith("<@&") -> {
                try {
                    config.addRoleToPermission(mentionable.idLong, RobertifyPermissionKt.ROBERTIFY_DJ)
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        PermissionsMessages.DJ_SET,
                        Pair("{mentionable}", mentionable.asMention)
                    ).build()
                } catch (e: IllegalAccessException) {
                    RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                        Pair("{mentionable}", mentionable.asMention),
                        Pair("{permission}", RobertifyPermissionKt.ROBERTIFY_DJ.name)
                    ).build()
                }
            }

            mentionable.asMention.startsWith("<@") -> {
                if (config.userHasPermission(mentionable.idLong, RobertifyPermissionKt.ROBERTIFY_DJ))
                    return RobertifyEmbedUtilsKt.embedMessage(
                        guild,
                        PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                        Pair("{mentionable}", mentionable.asMention),
                        Pair("{permission}", RobertifyPermissionKt.ROBERTIFY_DJ.name)
                    ).build()

                config.addPermissionToUser(mentionable.idLong, RobertifyPermissionKt.ROBERTIFY_DJ)
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    PermissionsMessages.DJ_SET,
                    Pair("{mentionable}", mentionable.asMention)
                ).build()
            }

            else -> RobertifyEmbedUtilsKt
                .embedMessage(guild, GeneralMessages.INVALID_ARGS)
                .build()
        }
    }
}