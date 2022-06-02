package main.commands.slashcommands.commands.management.permissions;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
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
        final var localeManager = LocaleManager.getLocaleManager(guild);

        List<String> roles = new PermissionsCommand().getRolePerms(event.getGuild(), Permission.ROBERTIFY_DJ.name());
        List<String> users = new PermissionsCommand().getUserPerms(event.getGuild(), Permission.ROBERTIFY_DJ.name());

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.PERMISSION_LIST, Pair.of("{permission}", Permission.ROBERTIFY_DJ.name()))
                .addField(localeManager.getMessage(RobertifyLocaleMessage.PermissionsMessages.PERMISSIONS_ROLES), roles.isEmpty() ? localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NOTHING_HERE) : GeneralUtils.listToString(roles), false)
                .addField(localeManager.getMessage(RobertifyLocaleMessage.PermissionsMessages.PERMISSIONS_USERS), users.isEmpty() ? localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NOTHING_HERE) : GeneralUtils.listToString(users), false);
        event.replyEmbeds(eb.build())
                .setEphemeral(false)
                .queue();
    }
}
