package main.commands.commands.management;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.main.Listener;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.json.guildconfig.GuildConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class UnbanCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_BAN)) {
            ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You do not have permission to run this command!\n\n" +
                                    "You must have `"+Permission.ROBERTIFY_BAN.name()+"`")
                            .build())
                    .queue();
            return;
        }

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a user to unban!").build())
                    .queue();
            return;
        }

        final var id = GeneralUtils.getDigitsOnly(args.get(0));

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user to unban.").build())
                    .queue();
            return;
        }

        final Member member = Robertify.api.getGuildById(ctx.getGuild().getIdLong()).getMemberById(id);

        if (member == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user to unban.").build())
                    .queue();
            return;
        }

        msg.replyEmbeds(handleUnban(ctx.getGuild(), member.getUser()).build())
                .queue();
    }

    private EmbedBuilder handleUnban(Guild guild, User user) {
        if (!new GuildConfig().isBannedUser(guild.getIdLong(), user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, "This user is not banned.");

        new GuildConfig().unbanUser(guild.getIdLong(), user.getIdLong());

        user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have been unbanned from **" + guild.getName() + "**").build())
                .queue(success -> {}, new ErrorHandler()
                        .handle(
                                ErrorResponse.CANNOT_SEND_TO_USER,
                                e -> Listener.logger.warn("Was not able to send an unban message to " + user.getAsTag() + "(" + user.getIdLong() + ")")
                        )));

        return RobertifyEmbedUtils.embedMessage(guild, "You have unbanned " + user.getAsMention());
    }

    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`" +
                "\nUnban a user from the bot\n\n" +
                "Usage: `"+ prefix +"unban <user>`";
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
                        "Unban a user from the bot",
                        List.of(CommandOption.of(
                                OptionType.USER,
                                "user",
                                "The user to unban",
                                true
                        )),
                        e -> GeneralUtils.hasPerms(e.getGuild(), e.getMember(), Permission.ROBERTIFY_BAN),
                        true
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You do not have permission to run this command!\n\n" +
                                    "You must have `"+Permission.ROBERTIFY_BAN.name()+"`")
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var userToUnban = event.getOption("user").getAsUser();

        event.replyEmbeds(handleUnban(event.getGuild(), userToUnban).build())
                .setEphemeral(false)
                .queue();
    }
}
