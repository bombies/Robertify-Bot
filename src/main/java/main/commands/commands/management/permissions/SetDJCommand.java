package main.commands.commands.management.permissions;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.database.ServerUtils;
import main.utils.json.permissions.PermissionsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.List;

public class SetDJCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final User sender = ctx.getAuthor();
        final Guild guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed("Set DJ");

        EmbedBuilder eb;

        if (!GeneralUtils.hasPerms(guild, sender, Permission.ROBERTIFY_ADMIN)) {
            eb = EmbedUtils.embedMessage("You don't have permission to run this command!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (args.isEmpty()) {
            eb = EmbedUtils.embedMessage("You must provide a role to set as a DJ role!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        String id = GeneralUtils.getDigitsOnly(args.get(0));

        Role role = guild.getRoleById(id);

        if (!GeneralUtils.stringIsID(id) || role == null) {
            eb = EmbedUtils.embedMessage("Please provide a valid role ID!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        PermissionsConfig permissionsConfig = new PermissionsConfig();
        try {
            permissionsConfig.addRoleToPermission(guild.getId(), id, Permission.ROBERTIFY_DJ);
            eb = EmbedUtils.embedMessage("Set " + role.getAsMention() + " as a DJ!");
            msg.replyEmbeds(eb.build()).queue();
        } catch (IllegalAccessException e) {
            eb = EmbedUtils.embedMessage("This role has already been set as a DJ!");
            msg.replyEmbeds(eb.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            eb = EmbedUtils.embedMessage("An unexpected error occurred!");
            msg.replyEmbeds(eb.build()).queue();
        }

        GeneralUtils.setDefaultEmbed();
    }

    @Override
    public String getName() {
        return "setdj";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Set a specific role to be a DJ\n\n" +
                "Usage: `"+ ServerUtils.getPrefix(Long.parseLong(guildID)) +"setdj <@role>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sdj");
    }
}
