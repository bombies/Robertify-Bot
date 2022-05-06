package main.commands.slashcommands.commands.management.permissions;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
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

public class SetDJCommand extends AbstractSlashCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(SetDJCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed(guild, "Set DJ");

        EmbedBuilder eb;

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You don't have permission to run this command!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (args.isEmpty()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a user/role to set as a DJ role!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        String id = GeneralUtils.getDigitsOnly(args.get(0));

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "That is an invalid ID!\nMake sure to either **mention** a user or provide their **ID**")
                            .setImage("https://i.imgur.com/V6pbhEZ.png")
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
            msg.replyEmbeds(handleSetDJ(guild, role).build()).queue();
        else
            msg.replyEmbeds(handleSetDJ(guild, user).build()).queue();

        GeneralUtils.setDefaultEmbed(guild);
    }

    private EmbedBuilder handleSetDJ(Guild guild, Role role) {
        EmbedBuilder eb;
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        try {
            permissionsConfig.addRoleToPermission(guild.getIdLong(), role.getIdLong(), Permission.ROBERTIFY_DJ);
            eb = RobertifyEmbedUtils.embedMessage(guild, "Set " + role.getAsMention() + " as a DJ!");
            return eb;
        } catch (IllegalAccessException e) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "This role has already been set as a DJ!");
            return eb;
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            eb = RobertifyEmbedUtils.embedMessage(guild, "An unexpected error occurred!");
            return eb;
        }
    }

    private EmbedBuilder handleSetDJ(Guild guild, User user) {
        EmbedBuilder eb;
        PermissionsConfig permissionsConfig = new PermissionsConfig();
        try {
            permissionsConfig.addPermissionToUser(guild.getIdLong(), user.getIdLong(), Permission.ROBERTIFY_DJ);
            eb = RobertifyEmbedUtils.embedMessage(guild, "Set " + user.getAsMention() + " as a DJ!");
            return eb;
        } catch (IllegalArgumentException e) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "This user has already been set as a DJ!");
            return eb;
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            eb = RobertifyEmbedUtils.embedMessage(guild, "An unexpected error occurred!");
            return eb;
        }
    }

    @Override
    public String getName() {
        return "setdj";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Set a specific role to be a DJ\n\n" +
                "Usage: `"+ prefix +"setdj <@role|@user>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sdj");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("setdj")
                        .setDescription("Set a specific user/role as a DJ!")
                        .addSubCommands(
                                SubCommand.of(
                                        "role",
                                        "Set a role as a DJ",
                                        List.of(CommandOption.of(
                                                OptionType.ROLE,
                                                "role",
                                                "The role to set as a DJ",
                                                true
                                        ))
                                ),
                                SubCommand.of(
                                        "user",
                                        "Set a user as a DJ",
                                        List.of(CommandOption.of(
                                                OptionType.USER,
                                                "user",
                                                "The user to set as a DJ",
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
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        GeneralUtils.setCustomEmbed(event.getGuild(), "Set DJ");
        switch  (event.getSubcommandName()) {
            case "role" -> {
                var role = event.getOption("role").getAsRole();
                event.replyEmbeds(handleSetDJ(event.getGuild(), role).build())
                        .setEphemeral(false)
                        .queue();
            }
            case "user" -> {
                var user = event.getOption("user").getAsUser();
                event.replyEmbeds(handleSetDJ(event.getGuild(), user).build())
                        .setEphemeral(false)
                        .queue();
            }
        }
        GeneralUtils.setDefaultEmbed(event.getGuild());
    }
}
