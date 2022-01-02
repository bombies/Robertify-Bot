package main.commands.commands.management.permissions;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class ListDJCommand extends InteractiveCommand implements ICommand {
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
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        getName(),
                        "View all the DJs in this server",
                        djPredicate
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.embedMessage("You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        List<String> roles = new PermissionsCommand().getRolePerms(event.getGuild(), Permission.ROBERTIFY_DJ.name());
        List<String> users = new PermissionsCommand().getUserPerms(event.getGuild(), Permission.ROBERTIFY_DJ.name());

        EmbedBuilder eb = EmbedUtils.embedMessage("**List of roles/users with permission** `" + Permission.ROBERTIFY_DJ.name() + "`")
                .addField("Roles", roles.isEmpty() ? "There is nothing here!" : GeneralUtils.listToString(roles), false)
                .addField("Users", users.isEmpty() ? "There is nothing here!" : GeneralUtils.listToString(users), false);

        event.replyEmbeds(eb.build())
                .setEphemeral(false)
                .queue();
    }
}
