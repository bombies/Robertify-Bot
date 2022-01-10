package main.commands.commands.management.permissions;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.permissions.PermissionsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionsCommand implements ICommand {
    
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
            new PermissionsConfig().addRoleToPermission(guild.getIdLong(), role.getIdLong(), Permission.valueOf(perm));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Added permission `"+perm+"` to: "+ role.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalAccessException e) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, role.getAsMention() + " already has permission to `"+perm+"`!");
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
            new PermissionsConfig().addPermissionToUser(guild.getIdLong(), user.getIdLong(), Permission.valueOf(perm));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Added permission `"+perm+"` to: "+ user.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalArgumentException e) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, user.getAsMention() + " already has permission to `"+perm+"`!");
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
            new PermissionsConfig().removePermissionFromUser(guild.getIdLong(), user.getIdLong(), Permission.valueOf(perm));
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
            new PermissionsConfig().removeRoleFromPermission(guild.getIdLong(), role.getIdLong(), Permission.valueOf(perm));
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Removed permission `"+perm+"` from: "+ role.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IOException e) {
            logger.error("[FATAL ERROR] There was an error reading from/writing to the JSON file!", e);
            msg.addReaction("❌").queue();
        } catch (IllegalAccessException e) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, role.getAsMention() + " does not have permission to `"+perm+"`!");
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
                eb = RobertifyEmbedUtils.embedMessage(guild, "There are no permissions yet!");
            else
                eb = RobertifyEmbedUtils.embedMessage(guild, "**List of Permissions**\n\n`" + GeneralUtils.listToString(perms) + "`");
            msg.replyEmbeds(eb.build()).queue();
        } else {
            PermissionsConfig permissionsConfig = new PermissionsConfig();

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
                    List<Integer> permCodes = permissionsConfig.getPermissionsForRoles(msg.getGuild().getIdLong(), role.getIdLong());
                    sendPermMessage(permCodes, msg, role);
                } else {
                    List<Integer> permCodes = permissionsConfig.getPermissionsForUser(msg.getGuild().getIdLong(), user.getIdLong());
                    sendPermMessage(permCodes, msg, user);
                }
            } else {
                if (Permission.getPermissions().contains(args.get(1).toUpperCase())) {
                    List<String> roles = getRolePerms(msg.getGuild(), args.get(1).toUpperCase());
                    List<String> users = getUserPerms(msg.getGuild(), args.get(1).toUpperCase());

                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "**List of roles/users with permission** `" + args.get(1).toUpperCase() + "`")
                            .addField("Roles", roles.isEmpty() ? "There is nothing here!" : GeneralUtils.listToString(roles), false)
                            .addField("Users", users.isEmpty() ? "There is nothing here!" : GeneralUtils.listToString(users), false);

                    msg.replyEmbeds(eb.build()).queue();
                } else {
                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Invalid permission!");
                    msg.replyEmbeds(eb.build()).queue();
                }
            }
        }
    }

    protected List<String> getRolePerms(Guild guild, String perm) {
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        List<Role> roles = new ArrayList<>();
        for (long s : permissionsConfig.getRolesForPermission(guild.getIdLong(), perm.toUpperCase()))
            roles.add(guild.getRoleById(s));

        List<String> rolesWithPerms = new ArrayList<>();
        for (Role r : roles)
            rolesWithPerms.add(r.getAsMention());

        return rolesWithPerms;
    }

    protected List<String> getUserPerms(Guild guild, String perm) {
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        List<User> users = new ArrayList<>();
        for (long s : permissionsConfig.getUsersForPermission(guild.getIdLong(), perm.toUpperCase()))
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
}
