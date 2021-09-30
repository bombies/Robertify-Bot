package main.commands.commands.dev.permissions;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.utils.GeneralUtils;
import main.utils.json.permissions.PermissionsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import javax.script.ScriptException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionsCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_ADMIN))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        GeneralUtils.setCustomEmbed("Robertify | Permissions", new Color(109, 254, 99));

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide arguments!");
            msg.replyEmbeds(eb.build()).queue();
            GeneralUtils.setDefaultEmbed();
            return;
        }

        switch (args.get(0).toLowerCase()) {
            case "init" -> init(msg);
            case "add" -> add(msg, args);
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
    private void init(Message msg) {
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
    private void add(Message msg, List<String> args) {
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

                if (role == null) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("There was no role found with that ID!");
                    msg.replyEmbeds(eb.build()).queue();
                    return;
                }

                List<Integer> permCodes = permissionsConfig.getPermissionsForRoles(msg.getGuild().getId(), role.getId());

                if (permCodes.isEmpty()) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("This role has no permissions!");
                    msg.replyEmbeds(eb.build()).queue();
                } else {
                    List<String> permString = new ArrayList<>();
                    for (int i : permCodes)
                        permString.add(Permission.getPermissions().get(i));
                    EmbedBuilder eb = EmbedUtils.embedMessage("**Permissions for** "+ role.getAsMention() + "\n\n`"+ permString +"`");
                    msg.replyEmbeds(eb.build()).queue();
                }
            } else {
                if (Permission.getPermissions().contains(args.get(1).toUpperCase())) {
                    List<Role> roles = new ArrayList<>();
                    for (String s : permissionsConfig.getRolesForPermission(msg.getGuild().getId(), args.get(1).toUpperCase()))
                        roles.add(msg.getGuild().getRoleById(s));

                    List<String> rolesString = new ArrayList<>();
                    for (Role r : roles)
                        rolesString.add(r.getAsMention());

                    EmbedBuilder eb = EmbedUtils.embedMessage("**List of roles with permission `" + args.get(1).toUpperCase()
                            + "`**\n\n" + rolesString);
                    msg.replyEmbeds(eb.build()).queue();
                } else {
                    EmbedBuilder eb = EmbedUtils.embedMessage("Invalid permission!");
                    msg.replyEmbeds(eb.build()).queue();
                }
            }
        }
    }

    @Override
    public String getName() {
        return "permissions";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("perms", "perm", "permission");
    }
}
