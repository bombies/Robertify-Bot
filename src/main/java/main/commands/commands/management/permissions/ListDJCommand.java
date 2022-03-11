package main.commands.commands.management.permissions;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class ListDJCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_DJ)) return;
        new PermissionsCommand().list(ctx.getMessage(), List.of("list", "robertify_dj"));
    }

    @Override
    public String getName() {
        return "listdj";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n\n" +
                "Looking to see who all the DJs are for this server? This is the command to use!";
    }

    @Override
    public List<String> getAliases() {
        return List.of("ldj", "djs");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("listdj")
                        .setDescription("View all the DJs in this server")
                        .setDJOnly()
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

        if (!djCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        List<String> roles = new PermissionsCommand().getRolePerms(event.getGuild(), Permission.ROBERTIFY_DJ.name());
        List<String> users = new PermissionsCommand().getUserPerms(event.getGuild(), Permission.ROBERTIFY_DJ.name());

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "**List of roles/users with permission** `" + Permission.ROBERTIFY_DJ.name() + "`")
                .addField("Roles", roles.isEmpty() ? "There is nothing here!" : GeneralUtils.listToString(roles), false)
                .addField("Users", users.isEmpty() ? "There is nothing here!" : GeneralUtils.listToString(users), false);

        event.replyEmbeds(eb.build())
                .setEphemeral(false)
                .queue();
    }
}
