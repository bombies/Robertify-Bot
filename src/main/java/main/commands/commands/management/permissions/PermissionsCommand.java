package main.commands.commands.management.permissions;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.database.BotDB;
import main.utils.database.ServerDB;
import main.utils.json.permissions.PermissionsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionsCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_ADMIN))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        GeneralUtils.setCustomEmbed("Permissions", new Color(109, 254, 99));

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide arguments!");
            msg.replyEmbeds(eb.build()).queue();
            GeneralUtils.setDefaultEmbed();
            return;
        }

        switch (args.get(0).toLowerCase()) {
            case "init" -> init(msg);
            case "add" -> add(msg, args);
            case "adduser", "addu", "au" -> addUser(msg, args);
            case "removeuser", "remu", "ru" -> removeUser(msg, args);
            case "remove" -> remove(msg, args);
            case "list" -> list(msg, args);
            default -> {
                EmbedBuilder eb = EmbedUtils.embedMessage("Invalid argument!");
                msg.replyEmbeds(eb.build()).queue();
            }
        }
        GeneralUtils.setDefaultEmbed();
    }

    /**
     * Initialize the permission JSON files for all servers
     * @param msg Message sent with the command
     */
    @SneakyThrows
    private void init(Message msg) {
        if (!new BotDB().isDeveloper(msg.getAuthor().getId()))
            return;

        try {
            new PermissionsConfig().initConfig();
            msg.addReaction("✅").queue();
        } catch (Exception e) {
            e.printStackTrace();
            msg.addReaction("❌").queue();
        }
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

        try {
            new PermissionsConfig().addRoleToPermission(msg.getGuild().getId(), role.getId(), Permission.valueOf(perm));
            EmbedBuilder eb = EmbedUtils.embedMessage("Added permission `"+perm+"` to: "+ role.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalAccessException e) {
            EmbedBuilder eb = EmbedUtils.embedMessage(role.getAsMention() + " already has permission to `"+perm+"`!");
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            msg.addReaction("❌").queue();
        }
    }

    private void addUser(Message msg, List<String> args) {
        Object[] checks = userChecks(msg, args);
        if (!((boolean) checks[0])) return;
        User user = (User) checks[1];
        String perm = (String) checks[2];

        try {
            new PermissionsConfig().addPermissionToUser(msg.getGuild().getId(), user.getId(), Permission.valueOf(perm));
            EmbedBuilder eb = EmbedUtils.embedMessage("Added permission `"+perm+"` to: "+ user.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalArgumentException e) {
            EmbedBuilder eb = EmbedUtils.embedMessage(user.getAsMention() + " already has permission to `"+perm+"`!");
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            msg.addReaction("❌").queue();
        }
    }

    private void removeUser(Message msg, List<String> args) {
        Object[] checks = userChecks(msg, args);
        if (!((boolean) checks[0])) return;
        User user = (User) checks[1];
        String perm = (String) checks[2];

        try {
            new PermissionsConfig().removePermissionFromUser(msg.getGuild().getId(), user.getId(), Permission.valueOf(perm));
            EmbedBuilder eb = EmbedUtils.embedMessage("Removed permission `"+perm+"` from: "+ user.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalArgumentException e) {
            EmbedBuilder eb = EmbedUtils.embedMessage(user.getAsMention() + " doesn't have permission to `"+perm+"`!");
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            msg.addReaction("❌").queue();
        }
    }

    private Object[] userChecks(Message msg, List<String> args) {
        if (args.size() < 2) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a user!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else if (args.size() < 3) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a permission to add to the user!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        }

        String id = GeneralUtils.getDigitsOnly(args.get(1));
        String perm = args.get(2).toUpperCase();

        User user;
        if (!GeneralUtils.stringIsID(id)) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid user!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else {
            user = Robertify.api.getUserById(id);
            if (user == null) {
                EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid role ID!");
                msg.replyEmbeds(eb.build()).queue();
                return new Object[] { false, null, null };
            }
        }

        if (!Permission.getPermissions().contains(perm)) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Invalid permission!\n" +
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

        try {
            new PermissionsConfig().removeRoleFromPermission(msg.getGuild().getId(), role.getId(), Permission.valueOf(perm));
            EmbedBuilder eb = EmbedUtils.embedMessage("Removed permission `"+perm+"` from: "+ role.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
            msg.addReaction("❌").queue();
        } catch (IllegalAccessException e) {
            EmbedBuilder eb = EmbedUtils.embedMessage(role.getAsMention() + " does not have permission to `"+perm+"`!");
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
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
        if (args.size() < 2) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a role!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else if (args.size() < 3) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a permission to add to the role!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        }

        String id = GeneralUtils.getDigitsOnly(args.get(1));
        String perm = args.get(2).toUpperCase();

        Role role;
        if (!GeneralUtils.stringIsID(id)) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid role ID!");
            msg.replyEmbeds(eb.build()).queue();
            return new Object[] { false, null, null };
        } else {
            role = msg.getGuild().getRoleById(id);
            if (role == null) {
                EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid role ID!");
                msg.replyEmbeds(eb.build()).queue();
                return new Object[] { false, null, null };
            }
        }

        if (!Permission.getPermissions().contains(perm)) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Invalid permission!\n" +
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
    private void list(Message msg, List<String> args) {
        if (args.size() == 1) {
            List<String> perms = Permission.getPermissions();
            EmbedBuilder eb;
            if (perms.isEmpty())
                eb = EmbedUtils.embedMessage("There are no permissions yet!");
            else
                eb = EmbedUtils.embedMessage("**List of Permissions**\n\n`" + perms + "`");
            msg.replyEmbeds(eb.build()).queue();
        } else {
            PermissionsConfig permissionsConfig = new PermissionsConfig();

            if (GeneralUtils.stringIsID(GeneralUtils.getDigitsOnly(args.get(1)))) {
                String id = GeneralUtils.getDigitsOnly(args.get(1));
                Role role = msg.getGuild().getRoleById(id);
                User user = Robertify.api.getUserById(id);

                if (role == null && user == null) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("There was no role or user found with that ID!");
                    msg.replyEmbeds(eb.build()).queue();
                    return;
                }

                if (user == null) {
                    List<Integer> permCodes = permissionsConfig.getPermissionsForRoles(msg.getGuild().getId(), role.getId());
                    sendPermMessage(permCodes, msg, role);
                } else {
                    List<Integer> permCodes = permissionsConfig.getPermissionsForUser(msg.getGuild().getId(), user.getId());
                    sendPermMessage(permCodes, msg, user);
                }
            } else {
                if (Permission.getPermissions().contains(args.get(1).toUpperCase())) {
                    List<Role> roles = new ArrayList<>();
                    List<User> users = new ArrayList<>();
                    for (String s : permissionsConfig.getRolesForPermission(msg.getGuild().getId(), args.get(1).toUpperCase()))
                        roles.add(msg.getGuild().getRoleById(s));
                    for (String s : permissionsConfig.getUsersForPermission(msg.getGuild().getId(), args.get(1).toUpperCase()))
                        users.add(Robertify.api.getUserById(s));

                    List<String> rolesString = new ArrayList<>();
                    for (Role r : roles)
                        rolesString.add(r.getAsMention());
                    for (User u : users)
                        rolesString.add(u.getAsMention());

                    if (!roles.isEmpty()) {
                        EmbedBuilder eb = EmbedUtils.embedMessage("**List of roles/users with permission `" + args.get(1).toUpperCase()
                                + "`**\n\n" + rolesString);
                        msg.replyEmbeds(eb.build()).queue();
                    } else {
                        EmbedBuilder eb = EmbedUtils.embedMessage("**List of roles/users with permission `" + args.get(1).toUpperCase()
                                + "`**\n\n`Nothing's here!`");
                        msg.replyEmbeds(eb.build()).queue();
                    }
                } else {
                    EmbedBuilder eb = EmbedUtils.embedMessage("Invalid permission!");
                    msg.replyEmbeds(eb.build()).queue();
                }
            }
        }
    }

    private void sendPermMessage(List<Integer> permCodes, Message msg, IMentionable mentionable) {
        if (permCodes.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("This user/role has no permissions!");
            msg.replyEmbeds(eb.build()).queue();
        } else {
            List<String> permString = new ArrayList<>();
            for (int i : permCodes)
                permString.add(Permission.getPermissions().get(i));
            EmbedBuilder eb = EmbedUtils.embedMessage("**Permissions for** " + mentionable.getAsMention() + "\n\n`" + permString + "`");
            msg.replyEmbeds(eb.build()).queue();
        }
    }

    @Override
    public String getName() {
        return "permissions";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Manage bot permissions for roles\n\n" +
                "\nUsage: `"+ ServerDB.getPrefix(Long.parseLong(guildID))+"permissions add <@role> <" + String.join(
                        "|",
                        Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ ServerDB.getPrefix(Long.parseLong(guildID))+"permissions adduser <@user> <" + String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ ServerDB.getPrefix(Long.parseLong(guildID))+"permissions remove <@role> <" + String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ ServerDB.getPrefix(Long.parseLong(guildID))+"permissions removeuser <@user> <" + String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) + ">`\n" +
                "\nUsage: `"+ ServerDB.getPrefix(Long.parseLong(guildID))+"permissions list <["+ String.join(
                "|",
                Permission.getPermissions().toString().replaceAll("[\\[\\]]", "").split(",\\s")
                ) +"]|@role|@user>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("perms", "perm", "permission");
    }
}
