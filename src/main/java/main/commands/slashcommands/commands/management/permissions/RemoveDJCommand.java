package main.commands.slashcommands.commands.management.permissions;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.permissions.PermissionsConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class RemoveDJCommand extends AbstractSlashCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(RemoveDJCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        EmbedBuilder eb;

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS_NO_ARGS);
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
    }

    private EmbedBuilder handleDJRemove(Guild guild, Role role) {
        PermissionsConfig permissionsConfig = new PermissionsConfig(guild);
        try {
            permissionsConfig.removeRoleFromPermission(role.getIdLong(), Permission.ROBERTIFY_DJ);
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.DJ_REMOVED, Pair.of("{mentionable}", role.getAsMention()));
        } catch (IllegalAccessException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.NOT_DJ, Pair.of("{djType}", "role"));
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR);
        }
    }

    private EmbedBuilder handleDJRemove(Guild guild, User user) {
        PermissionsConfig permissionsConfig = new PermissionsConfig(guild);
        try {
            permissionsConfig.removePermissionFromUser(user.getIdLong(), Permission.ROBERTIFY_DJ);
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.DJ_REMOVED, Pair.of("{mentionable}", user.getAsMention()));
        } catch (IllegalArgumentException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PermissionsMessages.NOT_DJ, Pair.of("{djType}", "user"));
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR);
        }
    }

    @Override
    public String getName() {
        return "removedj";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases())+"`\n" +
                "Remove DJ privileges from a specific role\n\n" +
                "Usage: `"+ prefix +"remove <@role|@user>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rdj");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("removedj")
                        .setDescription("Remove a role/user as a DJ")
                        .addSubCommands(
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
                        )
                        .setAdminOnly()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        switch  (event.getSubcommandName()) {
            case "role" -> {
                var role = event.getOption("role").getAsRole();
                event.replyEmbeds(handleDJRemove(event.getGuild(), role).build())
                        .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                        .queue();
            }
            case "user" -> {
                var user = event.getOption("user").getAsUser();
                event.replyEmbeds(handleDJRemove(event.getGuild(), user).build())
                        .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                        .queue();
            }
        }
    }
}
