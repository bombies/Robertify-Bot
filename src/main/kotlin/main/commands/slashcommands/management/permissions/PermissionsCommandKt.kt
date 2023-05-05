package main.commands.slashcommands.management.permissions

import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.RobertifyPermissionKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.toMention
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbeds
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandGroupKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.permissions.PermissionsConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PermissionsMessages
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class PermissionsCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "permissions",
        description = "Manage the permissions for roles and users.",
        adminOnly = true,
        subCommandGroups = listOf(
            SubCommandGroupKt(
                name = "list",
                description = "List the permissions assigned to your users and roles.",
                subCommands = listOf(
                    SubCommandKt(
                        name = "all",
                        description = "List all the roles and users with each permission."
                    ),
                    SubCommandKt(
                        name = "role",
                        description = "List the permissions a specific role has",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.ROLE,
                                name = "role",
                                description = "The role to query."
                            )
                        )
                    ),
                    SubCommandKt(
                        name = "user",
                        description = "List the permissions a specific user has",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.USER,
                                name = "user",
                                description = "The user to query."
                            )
                        )
                    )
                )
            ),
            SubCommandGroupKt(
                name = "add",
                description = "Add permissions to a user or role.",
                subCommands = listOf(
                    SubCommandKt(
                        "role",
                        description = "Add a specific permission to a specific role.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.ROLE,
                                name = "role",
                                description = "The role to add the permission to."
                            ),
                            CommandOptionKt(
                                name = "permission",
                                description = "The permission to add to the role.",
                                choices = RobertifyPermissionKt.permissions
                            )
                        )
                    ),
                    SubCommandKt(
                        "user",
                        description = "Add a specific permission to a specific user.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.USER,
                                name = "user",
                                description = "The user to add the permission to."
                            ),
                            CommandOptionKt(
                                name = "permission",
                                description = "The permission to add to the role.",
                                choices = RobertifyPermissionKt.permissions
                            )
                        )
                    )
                )
            ),
            SubCommandGroupKt(
                name = "remove",
                description = "Remove permissions from a user or role.",
                subCommands = listOf(
                    SubCommandKt(
                        "role",
                        description = "Remove a specific permission to a specific role.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.ROLE,
                                name = "role",
                                description = "The role to remove the permission from."
                            ),
                            CommandOptionKt(
                                name = "permission",
                                description = "The permission to remove from the role.",
                                choices = RobertifyPermissionKt.permissions
                            )
                        )
                    ),
                    SubCommandKt(
                        "user",
                        description = "Remove a specific permission from a specific user.",
                        options = listOf(
                            CommandOptionKt(
                                type = OptionType.USER,
                                name = "user",
                                description = "The user to remove the permission from."
                            ),
                            CommandOptionKt(
                                name = "permission",
                                description = "The permission to remove from the role.",
                                choices = RobertifyPermissionKt.permissions
                            )
                        )
                    )
                )
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val (_, subCommandGroup, subCommandName) = event.fullCommandName.split(" ")

        when (subCommandGroup) {
            "list" -> handleList(event, subCommandName)
            "add" -> handleAdd(event, subCommandName)
            "remove" -> handleRemove(event, subCommandName)
        }
    }

    private fun handleList(event: SlashCommandInteractionEvent, subCommandName: String) {
        val guild = event.guild!!
        val config = PermissionsConfigKt(guild)
        val localeManager = LocaleManagerKt[guild]

        when (subCommandName) {
            "all" -> {
                val rolesForPerms = mutableMapOf<RobertifyPermissionKt, List<String>>()
                RobertifyPermissionKt.values().forEach { permission ->
                    rolesForPerms[permission] = getRolesForPermission(permission, config)
                }

                val usersForPerms = mutableMapOf<RobertifyPermissionKt, List<String>>()
                RobertifyPermissionKt.values().forEach { permission ->
                    usersForPerms[permission] = getUsersForPermission(permission, config)
                }

                event.replyWithEmbeds(guild) {
                    RobertifyPermissionKt.values().map { permission ->
                        embed(
                            """${
                                localeManager.getMessage(
                                    PermissionsMessages.PERMISSION_LIST, Pair(
                                        "{permission}",
                                        permission.name
                                    )
                                )
                            }
                            
                            ${
                                if (usersForPerms[permission]!!.isEmpty() && rolesForPerms[permission]!!.isEmpty())
                                    localeManager.getMessage(GeneralMessages.NOTHING_HERE)
                                else
                                    usersForPerms[permission]!!.joinToString(", ") +
                                            (if (usersForPerms[permission]!!.isNotEmpty()) "\n" else "") +
                                            rolesForPerms[permission]!!.joinToString(", ")
                            }
                            
                            """
                        )

                    }
                }.queue()
            }

            "role" -> {
                val role = event.getRequiredOption("role").asRole
                val perms = config.getPermissionsForRoles(role.idLong).map { RobertifyPermissionKt.parse(it) }
                displaySpecificMentionable(event, role, perms)
            }

            "user" -> {
                val user = event.getRequiredOption("user").asUser
                val perms = config.getPermissionsForUser(user.idLong).map { RobertifyPermissionKt.parse(it) }
                displaySpecificMentionable(event, user, perms)
            }
        }
    }

    private fun displaySpecificMentionable(
        event: SlashCommandInteractionEvent,
        mentionable: IMentionable,
        perms: List<RobertifyPermissionKt>
    ) {
        val guild = event.guild!!

        if (perms.isEmpty())
            event.replyWithEmbed(guild) {
                embed(PermissionsMessages.MENTIONABLE_PERMISSIONS_NONE)
            }.queue()
        else
            event.replyWithEmbed(guild) {
                embed(
                    PermissionsMessages.MENTIONABLE_PERMISSIONS_LIST,
                    Pair("{mentionable}", mentionable.asMention),
                    Pair("{permissions}", perms.joinToString(", ") { it.name })
                )
            }.queue()
    }

    fun getRolesForPermission(permission: RobertifyPermissionKt, config: PermissionsConfigKt): List<String> {
        return config.getRolesForPermission(permission)
            .map { id -> id.toMention(GeneralUtilsKt.Mentioner.ROLE) }
    }

    fun getUsersForPermission(permission: RobertifyPermissionKt, config: PermissionsConfigKt): List<String> {
        return config.getUsersForPermission(permission)
            .map { id -> id.toMention(GeneralUtilsKt.Mentioner.USER) }
    }

    private fun handleAdd(event: SlashCommandInteractionEvent, subCommandName: String) {
        val config = PermissionsConfigKt(event.guild!!)
        val permission = RobertifyPermissionKt.valueOf(event.getRequiredOption("permission").asString)

        when (subCommandName) {
            "role" -> {
                val role = event.getRequiredOption("role").asRole
                try {
                    config.addRoleToPermission(role.idLong, permission)
                    displayPermissionAdded(event, role, permission)
                } catch (e: IllegalAccessException) {
                    displayAlreadyHasPermission(event, role, permission)
                }
            }

            "user" -> {
                val user = event.getRequiredOption("user").asUser
                if (config.userHasPermission(user.idLong, permission))
                    return displayAlreadyHasPermission(event, user, permission)

                config.addPermissionToUser(user.idLong, permission)
                displayPermissionAdded(event, user, permission)
            }
        }
    }

    private fun displayPermissionAdded(
        event: SlashCommandInteractionEvent,
        mentionable: IMentionable,
        permission: RobertifyPermissionKt
    ) {
        event.replyWithEmbed(event.guild!!) {
            embed(
                PermissionsMessages.PERMISSION_ADDED,
                Pair("{mentionable}", mentionable.asMention),
                Pair("{permission}", permission.name)
            )
        }.setEphemeral(true)
            .queue()
    }

    private fun displayAlreadyHasPermission(
        event: SlashCommandInteractionEvent,
        mentionable: IMentionable,
        permission: RobertifyPermissionKt
    ) {
        event.replyWithEmbed(event.guild!!) {
            embed(
                PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                Pair("{mentionable}", mentionable.asMention),
                Pair("{permission}", permission.name)
            )
        }.setEphemeral(true)
            .queue()
    }

    private fun handleRemove(event: SlashCommandInteractionEvent, subCommandName: String) {
        val config = PermissionsConfigKt(event.guild!!)
        val permission = RobertifyPermissionKt.valueOf(event.getRequiredOption("permission").asString)

        when (subCommandName) {
            "role" -> {
                val role = event.getRequiredOption("role").asRole
                try {
                    config.removeRoleFromPermission(role.idLong, permission)
                    displayPermissionRemoved(event, role, permission)
                } catch (e: IllegalAccessException) {
                    displayDoesntHavePermission(event, role, permission)
                }
            }

            "user" -> {
                val user = event.getRequiredOption("user").asUser
                if (!config.userHasPermission(user.idLong, permission))
                    return displayPermissionAdded(event, user, permission)

                config.removePermissionFromUser(user.idLong, permission)
                displayPermissionRemoved(event, user, permission)
            }
        }
    }

    private fun displayPermissionRemoved(
        event: SlashCommandInteractionEvent,
        mentionable: IMentionable,
        permission: RobertifyPermissionKt
    ) {
        event.replyWithEmbed(event.guild!!) {
            embed(
                PermissionsMessages.PERMISSION_REMOVED,
                Pair("{mentionable}", mentionable.asMention),
                Pair("{permission}", permission.name)
            )
        }.setEphemeral(true)
            .queue()
    }

    private fun displayDoesntHavePermission(
        event: SlashCommandInteractionEvent,
        mentionable: IMentionable,
        permission: RobertifyPermissionKt
    ) {
        event.replyWithEmbed(event.guild!!) {
            embed(
                PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                Pair("{mentionable}", mentionable.asMention),
                Pair("{permission}", permission.name)
            )
        }.setEphemeral(true)
            .queue()
    }

}