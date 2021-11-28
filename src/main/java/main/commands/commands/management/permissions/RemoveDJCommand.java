package main.commands.commands.management.permissions;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.ServerDB;
import main.utils.json.permissions.PermissionsConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class RemoveDJCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final User sender = ctx.getAuthor();
        final Guild guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed("Remove DJ");

        EmbedBuilder eb;

        if (!GeneralUtils.hasPerms(guild, sender, Permission.ROBERTIFY_ADMIN)) {
            eb = EmbedUtils.embedMessage("You don't have permission to run this command!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (args.isEmpty()) {
            eb = EmbedUtils.embedMessage("You must provide a role to remove as a DJ role!");
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

        msg.replyEmbeds(handleDJRemove(guild, role).build()).queue();

        GeneralUtils.setDefaultEmbed();
    }

    private EmbedBuilder handleDJRemove(Guild guild, Role role) {
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        try {
            permissionsConfig.removeRoleFromPermission(guild.getId(), role.getId(), Permission.ROBERTIFY_DJ);
            return EmbedUtils.embedMessage("Removed " + role.getAsMention() + " as a DJ!");
        } catch (IllegalAccessException e) {
            return EmbedUtils.embedMessage("This role never was a DJ in the first place");
        } catch (Exception e) {
            e.printStackTrace();
            return EmbedUtils.embedMessage("An unexpected error occurred!");
        }
    }

    @Override
    public String getName() {
        return "removedj";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Remove DJ privileges from a specific role\n\n" +
                "Usage: `"+ ServerDB.getPrefix(Long.parseLong(guildID)) +"remove <@role>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rdj");
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
                        "Remove a role asa DJ",
                        List.of(CommandOption.of(
                                OptionType.ROLE,
                                "role",
                                "The role to remove as a dj",
                                true
                        )),
                        (e) -> GeneralUtils.hasPerms(e.getGuild(), e.getUser(), Permission.ROBERTIFY_ADMIN)
                )).build();

    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (!getInteractionCommand().getCommand().permissionCheck(event)) {
            event.replyEmbeds(EmbedUtils.embedMessage("You need to be a DJ to use this command!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        GeneralUtils.setCustomEmbed("Remove DJ");
        var role = event.getOption("role").getAsRole();

        event.replyEmbeds(handleDJRemove(event.getGuild(), role).build())
                .setEphemeral(false)
                .queue();

        GeneralUtils.setDefaultEmbed();
    }
}
