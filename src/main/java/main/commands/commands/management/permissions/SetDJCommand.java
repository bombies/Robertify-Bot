package main.commands.commands.management.permissions;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.ServerUtils;
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

public class SetDJCommand extends InteractiveCommand implements ICommand {
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
        msg.replyEmbeds(handleSetDJ(guild, role).build()).queue();

        GeneralUtils.setDefaultEmbed();
    }

    private EmbedBuilder handleSetDJ(Guild guild, Role role) {
        EmbedBuilder eb;
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        try {
            permissionsConfig.addRoleToPermission(guild.getId(), role.getId(), Permission.ROBERTIFY_DJ);
            eb = EmbedUtils.embedMessage("Set " + role.getAsMention() + " as a DJ!");
            return eb;
        } catch (IllegalAccessException e) {
            eb = EmbedUtils.embedMessage("This role has already been set as a DJ!");
            return eb;
        } catch (Exception e) {
            e.printStackTrace();
            eb = EmbedUtils.embedMessage("An unexpected error occurred!");
            return eb;
        }
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
                        "Set a specific as a DJ!",
                        List.of(CommandOption.of(
                                OptionType.ROLE,
                                "role",
                                "The role to set as a DJ",
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

        var role = event.getOption("role").getAsRole();

        GeneralUtils.setCustomEmbed("Set DJ");

        event.replyEmbeds(handleSetDJ(event.getGuild(), role).build())
                .setEphemeral(false)
                .queue();

        GeneralUtils.setDefaultEmbed();

    }
}
