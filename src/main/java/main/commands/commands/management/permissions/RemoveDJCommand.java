package main.commands.commands.management.permissions;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.json.permissions.PermissionsConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class RemoveDJCommand extends InteractiveCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(RemoveDJCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final User sender = ctx.getAuthor();
        final Guild guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed(guild, "Remove DJ");

        EmbedBuilder eb;

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You don't have permission to run this command!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (args.isEmpty()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a role to remove as a DJ role!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        String id = GeneralUtils.getDigitsOnly(args.get(0));

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That is an invalid ID!\nMake sure to either **mention** a user or provide their **ID**")
                            .setImage("https://i.imgur.com/wa8CjnJ.png")
                            .build())
                    .queue();
            return;
        }

        Role role = guild.getRoleById(id);
        User user = GeneralUtils.retrieveUser(id);

        if (role == null && user == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "Please provide a valid role/user ID!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (user == null)
            msg.replyEmbeds(handleDJRemove(guild, role).build()).queue();
        else
            msg.replyEmbeds(handleDJRemove(guild, user).build()).queue();

        GeneralUtils.setDefaultEmbed(guild);
    }

    private EmbedBuilder handleDJRemove(Guild guild, Role role) {
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        try {
            permissionsConfig.removeRoleFromPermission(guild.getIdLong(), role.getIdLong(), Permission.ROBERTIFY_DJ);
            return RobertifyEmbedUtils.embedMessage(guild, "Removed " + role.getAsMention() + " as a DJ!");
        } catch (IllegalAccessException e) {
            return RobertifyEmbedUtils.embedMessage(guild, "This role never was a DJ in the first place");
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            return RobertifyEmbedUtils.embedMessage(guild, "An unexpected error occurred!");
        }
    }

    private EmbedBuilder handleDJRemove(Guild guild, User user) {
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        try {
            permissionsConfig.removePermissionFromUser(guild.getIdLong(), user.getIdLong(), Permission.ROBERTIFY_DJ);
            return RobertifyEmbedUtils.embedMessage(guild, "Removed " + user.getAsMention() + " as a DJ!");
        } catch (IllegalArgumentException e) {
            return RobertifyEmbedUtils.embedMessage(guild, "This user never was a DJ in the first place");
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            return RobertifyEmbedUtils.embedMessage(guild, "An unexpected error occurred!");
        }
    }

    @Override
    public String getName() {
        return "removedj";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Remove DJ privileges from a specific role\n\n" +
                "Usage: `"+ prefix +"remove <@role|@user>`";
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
                        "Remove a role/user as a DJ",
                        List.of(),
                        List.of(
                                SubCommand.of(
                                        "role",
                                        "Remove a role as a DJ",
                                        List.of(CommandOption.of(
                                                OptionType.ROLE,
                                                "role",
                                                "The role to remove as a DJ",
                                                true
                                        ))
                                ),
                                SubCommand.of(
                                        "user",
                                        "Remove a user as a DJ",
                                        List.of(CommandOption.of(
                                                OptionType.USER,
                                                "user",
                                                "The user to remove as a DJ",
                                                true
                                        ))
                                )
                        ),
                        (e) -> GeneralUtils.hasPerms(e.getGuild(), e.getMember(), Permission.ROBERTIFY_ADMIN)
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to use this command!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        GeneralUtils.setCustomEmbed(event.getGuild(), "Remove DJ");
        switch  (event.getSubcommandName()) {
            case "role" -> {
                var role = event.getOption("role").getAsRole();
                event.replyEmbeds(handleDJRemove(event.getGuild(), role).build())
                        .setEphemeral(false)
                        .queue();
            }
            case "user" -> {
                var user = event.getOption("user").getAsUser();
                event.replyEmbeds(handleDJRemove(event.getGuild(), user).build())
                        .setEphemeral(false)
                        .queue();
            }
        }
        GeneralUtils.setDefaultEmbed(event.getGuild());
    }
}
