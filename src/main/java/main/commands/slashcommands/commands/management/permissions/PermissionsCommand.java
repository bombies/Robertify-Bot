package main.commands.slashcommands.commands.management.permissions;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.permissions.PermissionsConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionsCommand extends AbstractSlashCommand implements ICommand {
    
    private final Logger logger = LoggerFactory.getLogger(PermissionsCommand.class);
    
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_ADMIN))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed(guild, "Permissions", new Color(109, 254, 99));

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide arguments!");
            msg.replyEmbeds(eb.build()).queue();
            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        }

        switch (args.get(0).toLowerCase()) {
            case "add" -> add(msg, args);
            case "adduser", "addu", "au" -> addUser(msg, args);
            case "removeuser", "remu", "ru" -> removeUser(msg, args);
            case "remove" -> remove(msg, args);
            case "list" -> list(msg, args);
            default -> {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Invalid argument!");
                msg.replyEmbeds(eb.build()).queue();
            }
        }
        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    /**
     * Add a specific permission to a specific role
     * @param msg Message sent with the command
     * @param args Arguments with the role ID and the permission to be added
     */
    private void  add(Message msg, List<String> args) {
        Object[] checks = checks(msg, args);
        if (!((boolean) checks[0])) return;
        Role role = (Role) checks[1];
        String perm = (String) checks[2];

        final var guild = msg.getGuild();

        try {
            new PermissionsConfig(guild).addRoleToPermission(role.getIdLong(), Permission.valueOf(perm));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_ADDED,
                    Pair.of("{permission}", perm),
                    Pair.of("{mentionable}", role.getAsMention())
            );
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalAccessException e) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                    Pair.of("{permission}", perm),
                    Pair.of("{mentionable}", role.getAsMention())
            );
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            msg.addReaction("❌").queue();
        }
    }

    private void addUser(Message msg, List<String> args) {
        Object[] checks = userChecks(msg, args);
        if (!((boolean) checks[0])) return;
        User user = (User) checks[1];
        String perm = (String) checks[2];

        final var guild = msg.getGuild();

        try {
            new PermissionsConfig(guild).addPermissionToUser(user.getIdLong(), Permission.valueOf(perm));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_ADDED,
                    Pair.of("{permission}", perm),
                    Pair.of("{mentionable}", user.getAsMention())
            );
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalArgumentException e) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                    Pair.of("{permission}", perm),
                    Pair.of("{mentionable}", user.getAsMention())
            );
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            msg.addReaction("❌").queue();
        }
    }

    private void removeUser(Message msg, List<String> args) {
        Object[] checks = userChecks(msg, args);
        if (!((boolean) checks[0])) return;
        User user = (User) checks[1];
        String perm = (String) checks[2];

        final var guild = msg.getGuild();

        try {
            new PermissionsConfig(guild).removePermissionFromUser(user.getIdLong(), Permission.valueOf(perm));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Removed permission `"+perm+"` from: "+ user.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalArgumentException e) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, user.getAsMention() + " doesn't have permission to `"+perm+"`!");
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            msg.addReaction("❌").queue();
        }
    }

    private Object[] userChecks(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a user!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else if (args.size() < 3) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a permission to add to the user!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        }

        String id = GeneralUtils.getDigitsOnly(args.get(1));
        String perm = args.get(2).toUpperCase();

        User user;
        if (!GeneralUtils.stringIsID(id)) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else {
            user = GeneralUtils.retrieveUser(id);
            if (user == null) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid role ID!");
                msg.replyEmbeds(eb.build()).queue();
                return new Object[] { false, null, null };
            }
        }

        if (!Permission.getPermissions().contains(perm)) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Invalid permission!\n" +
                    "\n**Valid permissions**\n`"+ Permission.getPermissions() +"`");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        }

        return new Object[] { true, user, perm };
    }

    /**
     * Remove a specific permission to a specific role
     * @param msg Message sent with the command
     * @param args Arguments with the role ID and the permission to be removed
     */
    private void remove(Message msg, List<String> args) {
        Object[] checks = checks(msg, args);
        if (!((boolean) checks[0])) return;
        Role role = (Role) checks[1];
        String perm = (String) checks[2];

        final var guild = msg.getGuild();

        try {
            new PermissionsConfig(guild).removeRoleFromPermission(role.getIdLong(), Permission.valueOf(perm));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_REMOVED,
                    Pair.of("{mentionable}", role.getAsMention()),
                    Pair.of("{permission}", perm.toUpperCase())
            );
            msg.replyEmbeds(eb.build()).queue();
        } catch (IOException e) {
            logger.error("[FATAL ERROR] There was an IOException", e);
            msg.addReaction("❌").queue();
        } catch (IllegalAccessException e) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                    Pair.of("{mentionable}", role.getAsMention()),
                    Pair.of("{permission}", perm.toUpperCase())
            );
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            msg.addReaction("❌").queue();
        }
    }

    /**
     * Checks to be done before attempting to add/remove a permission to/from a role.
     * These checks ensure that no invalid inputs will be attempted to be added to the permissions file
     * @param msg Message which contains the command
     * @param args Arguments of the command
     * @return Array of objects containing if the checks were successful, the role object and the permission name object
     */
    private Object[] checks(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a role!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else if (args.size() < 3) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a permission to add to the role!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        }

        String id = GeneralUtils.getDigitsOnly(args.get(1));
        String perm = args.get(2).toUpperCase();

        Role role;
        if (!GeneralUtils.stringIsID(id)) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid role ID!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else {
            role = msg.getGuild().getRoleById(id);
            if (role == null) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid role ID!");
                msg.replyEmbeds(eb.build()).queue();
                return new Object[] { false, null, null };
            }
        }

        if (!Permission.getPermissions().contains(perm)) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Invalid permission!\n" +
                    "\n**Valid permissions**\n`"+ Permission.getPermissions() +"`");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        }

        return new Object[] { true, role, perm};
    }

    /**
     * Lists all the permissions for the bot
     * @param msg Message which contains the command
     * @param args List of arguments which can contain either the ID of a role or the name of a permission
     */
    protected void list(Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() == 1) {
            List<String> perms = Permission.getPermissions();
            EmbedBuilder eb;
            if (perms.isEmpty())
                eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSIONS_NONE);
            else
                eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSIONS_LIST, Pair.of("{permissions}", GeneralUtils.listToString(perms)));
            msg.replyEmbeds(eb.build()).queue();
        } else {
            PermissionsConfig permissionsConfig = new PermissionsConfig(guild);

            if (GeneralUtils.stringIsID(GeneralUtils.getDigitsOnly(args.get(1)))) {
                String id = GeneralUtils.getDigitsOnly(args.get(1));
                Role role = msg.getGuild().getRoleById(id);
                User user = GeneralUtils.retrieveUser(id);

                if (role == null && user == null) {
                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There was no role or user found with that ID!");
                    msg.replyEmbeds(eb.build()).queue();
                    return;
                }

                if (user == null) {
                    List<Integer> permCodes = permissionsConfig.getPermissionsForRoles(role.getIdLong());
                    sendPermMessage(permCodes, msg, role);
                } else {
                    List<Integer> permCodes = permissionsConfig.getPermissionsForUser(user.getIdLong());
                    sendPermMessage(permCodes, msg, user);
                }
            } else {
                if (Permission.getPermissions().contains(args.get(1).toUpperCase())) {
                    final var roles = getRolePerms(msg.getGuild(), args.get(1).toUpperCase());
                    final var users = getUserPerms(msg.getGuild(), args.get(1).toUpperCase());
                    final var localeManager = LocaleManager.getLocaleManager(guild);

                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_LIST, Pair.of("{permission}", args.get(1).toUpperCase()))
                            .addField(localeManager.getMessage(RobertifyLocaleMessage.PermissionsMessages.PERMISSIONS_ROLES), roles.isEmpty() ? localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NOTHING_HERE) : GeneralUtils.listToString(roles), false)
                            .addField(localeManager.getMessage(RobertifyLocaleMessage.PermissionsMessages.PERMISSIONS_USERS), users.isEmpty() ? localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NOTHING_HERE) : GeneralUtils.listToString(users), false);

                    msg.replyEmbeds(eb.build()).queue();
                } else {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.INVALID_PERMISSION);
                    msg.replyEmbeds(eb.build()).queue();
                }
            }
        }
    }

    protected List<String> getRolePerms(Guild guild, String perm) {
        PermissionsConfig permissionsConfig = new PermissionsConfig(guild);
        List<Role> roles = new ArrayList<>();
        for (long s : permissionsConfig.getRolesForPermission(perm.toUpperCase()))
            roles.add(guild.getRoleById(s));

        List<String> rolesWithPerms = new ArrayList<>();
        for (Role r : roles)
            rolesWithPerms.add(r.getAsMention());

        return rolesWithPerms;
    }

    protected List<String> getUserPerms(Guild guild, String perm) {
        PermissionsConfig permissionsConfig = new PermissionsConfig(guild);
        List<User> users = new ArrayList<>();
        for (long s : permissionsConfig.getUsersForPermission(perm.toUpperCase()))
            users.add(GeneralUtils.retrieveUser(s));

        List<String> usersWithPerm = new ArrayList<>();
        for (User u : users)
            usersWithPerm.add(u.getAsMention());

        return usersWithPerm;
    }

    private void sendPermMessage(List<Integer> permCodes, Message msg, IMentionable mentionable) {
        final var guild = msg.getGuild();

        if (permCodes.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "This user/role has no permissions!");
            msg.replyEmbeds(eb.build()).queue();
        } else {
            List<String> permString = new ArrayList<>();
            for (int i : permCodes)
                permString.add(Permission.getPermissions().get(i));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "**Permissions for** " + mentionable.getAsMention() + "\n\n`" + permString + "`");
            msg.replyEmbeds(eb.build()).queue();
        }
    }

    @Override
    public String getName() {
        return "permissions";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Manage bot permissions for roles\n\n" +
                "\nUsage: `"+ prefix+"permissions add <@role> <" + String.join(
                        "|",
                        Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ prefix+"permissions adduser <@user> <" + String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ prefix+"permissions remove <@role> <" + String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ prefix+"permissions removeuser <@user> <" + String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ prefix+"permissions list <["+ String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) +"]|@role|@user>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("perms", "perm", "permission");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("permissions")
                        .setDescription("Manage the permissions for roles and users")
                        .addSubCommandGroups(
                                SubCommandGroup.of(
                                        "list",
                                        "List permissions!",
                                        List.of(
                                                SubCommand.of(
                                                        "permissions",
                                                        "List all the valid permissions"
                                                ),
                                                SubCommand.of(
                                                        "role",
                                                        "All the permissions a specific role has",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.ROLE,
                                                                        "role",
                                                                        "The role to check",
                                                                        true
                                                                )
                                                        )
                                                ),
                                                SubCommand.of(
                                                        "user",
                                                        "All the permissions a specific user has",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.USER,
                                                                        "user",
                                                                        "The user to check",
                                                                        true
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                SubCommandGroup.of(
                                        "add",
                                        "Add permissions to a user or role!",
                                        List.of(
                                                SubCommand.of(
                                                        "role",
                                                        "Add a specific permission to a specific role",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.ROLE,
                                                                        "role",
                                                                        "The role to add the permission to",
                                                                        true
                                                                ),
                                                                CommandOption.of(
                                                                        OptionType.STRING,
                                                                        "permission",
                                                                        "The permission to add to the role",
                                                                        true,
                                                                        Permission.getPermissions()
                                                                )
                                                        )
                                                ),
                                                SubCommand.of(
                                                        "user",
                                                        "Add a specific permission to a specific user",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.USER,
                                                                        "user",
                                                                        "The user to add the permission to",
                                                                        true
                                                                ),
                                                                CommandOption.of(
                                                                        OptionType.STRING,
                                                                        "permission",
                                                                        "The permission to add to the user",
                                                                        true,
                                                                        Permission.getPermissions()
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                SubCommandGroup.of(
                                        "remove",
                                        "Remove permissions from a user or role!",
                                        List.of(
                                                SubCommand.of(
                                                        "role",
                                                        "Remove a specific permission from a specific role",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.ROLE,
                                                                        "role",
                                                                        "The role to add the permission to",
                                                                        true
                                                                ),
                                                                CommandOption.of(
                                                                        OptionType.STRING,
                                                                        "permission",
                                                                        "The permission to add to the role",
                                                                        true,
                                                                        Permission.getPermissions()
                                                                )
                                                        )
                                                ),
                                                SubCommand.of(
                                                        "user",
                                                        "Remove a specific permission from a specific user",
                                                        List.of(
                                                                CommandOption.of(
                                                                        OptionType.USER,
                                                                        "user",
                                                                        "The user to add the permission to",
                                                                        true
                                                                ),
                                                                CommandOption.of(
                                                                        OptionType.STRING,
                                                                        "permission",
                                                                        "The permission to add to the user",
                                                                        true,
                                                                        Permission.getPermissions()
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .setAdminOnly()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        final var guild = event.getGuild();
        final var config = new PermissionsConfig(guild);
        final var path = event.getCommandPath().split("/");

        switch (path[1]) {
            case "list" -> {
                switch (path[2]) {
                    case "permissions" -> {
                        List<String> perms = Permission.getPermissions();
                        EmbedBuilder eb;
                        if (perms.isEmpty())
                            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSIONS_NONE);
                        else
                            eb = RobertifyEmbedUtils.embedMessage(guild, "**List of Permissions**\n\n`" + GeneralUtils.listToString(perms) + "`");
                        event.replyEmbeds(eb.build())
                                .setEphemeral(true)
                                .queue();
                    }
                    case "role" -> {
                        final var role = event.getOption("role").getAsRole();
                        List<Integer> permCodes = config.getPermissionsForRoles(role.getIdLong());
                        sendPermMessage(event, permCodes, role);
                    }
                    case "user" -> {
                        final var user = event.getOption("user").getAsUser();
                        List<Integer> permCodes = config.getPermissionsForUser(user.getIdLong());
                        sendPermMessage(event, permCodes, user);
                    }
                }
            }
            case "add" -> {
                switch (path[2]) {
                    case "role" -> {
                        final var role = event.getOption("role").getAsRole();
                        final var perm = event.getOption("permission").getAsString();

                        try {
                            new PermissionsConfig(guild).addRoleToPermission(role.getIdLong(), Permission.valueOf(perm));
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_ADDED,
                                    Pair.of("{mentionable}", role.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (IllegalAccessException e) {
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                                    Pair.of("{mentionable}", role.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (Exception e) {
                            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
                            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR)
                                    .build())
                                    .setEphemeral(true)
                                    .queue();
                        }
                    }
                    case "user" -> {
                        final var user = event.getOption("user").getAsUser();
                        final var perm = event.getOption("permission").getAsString();

                        try {
                            new PermissionsConfig(guild).addPermissionToUser(user.getIdLong(), Permission.valueOf(perm));
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                                    Pair.of("{mentionable}", user.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (IllegalArgumentException e) {
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_ALREADY_HAS_PERMISSION,
                                    Pair.of("{mentionable}", user.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (Exception e) {
                            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
                            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR)
                                            .build())
                                    .setEphemeral(true)
                                    .queue();
                        }
                    }
                }
            }
            case "remove" -> {
                switch (path[2]) {
                    case "role" -> {
                        final var role = event.getOption("role").getAsRole();
                        final var perm = event.getOption("permission").getAsString();

                        try {
                            new PermissionsConfig(guild).removeRoleFromPermission(role.getIdLong(), Permission.valueOf(perm));
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_REMOVED,
                                    Pair.of("{mentionable}", role.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (IllegalAccessException e) {
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                                    Pair.of("{mentionable}", role.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (Exception e) {
                            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
                            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR)
                                            .build())
                                    .setEphemeral(true)
                                    .queue();
                        }
                    }
                    case "user" -> {
                        final var user = event.getOption("user").getAsUser();
                        final var perm = event.getOption("permission").getAsString();

                        try {
                            new PermissionsConfig(guild).removePermissionFromUser(user.getIdLong(), Permission.valueOf(perm));
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_REMOVED,
                                    Pair.of("{mentionable}", user.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (IllegalArgumentException e) {
                            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_NEVER_HAD_PERMISSION,
                                    Pair.of("{mentionable}", user.getAsMention()),
                                    Pair.of("{permission}", perm.toUpperCase())
                            );
                            event.replyEmbeds(eb.build())
                                    .setEphemeral(true)
                                    .queue();
                        } catch (Exception e) {
                            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
                            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR)
                                            .build())
                                    .setEphemeral(true)
                                    .queue();
                        }
                    }
                }
            }
        }
    }

    private void sendPermMessage(SlashCommandEvent event, List<Integer> permCodes, IMentionable mentionable) {
        final var guild = event.getGuild();

        if (permCodes.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_PERMISSIONS_NONE);
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
        } else {
            List<String> permString = permCodes.stream()
                    .map(i -> Permission.getPermissions().get(i))
                    .toList();
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.MENTIONABLE_PERMISSIONS_LIST,
                    Pair.of("{mentionable}", mentionable.getAsMention()),
                    Pair.of("{permissions}", GeneralUtils.listToString(permString))
            );
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
