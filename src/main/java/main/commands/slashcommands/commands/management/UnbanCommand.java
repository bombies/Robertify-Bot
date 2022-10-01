package main.commands.slashcommands.commands.management;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class UnbanCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_BAN)) {
            ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_BAN.name()))
                            .build())
                    .queue();
            return;
        }

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.MISSING_UNBAN_USER).build())
                    .queue();
            return;
        }

        final var id = GeneralUtils.getDigitsOnly(args.get(0));

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.INVALID_UNBAN_USER).build())
                    .queue();
            return;
        }

        final Member member;
        try {
            member = ctx.getGuild().retrieveMemberById(id).complete();
        } catch (ErrorResponseException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.INVALID_UNBAN_USER).build())
                    .queue();
            return;
        }

        if (member == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.INVALID_UNBAN_USER).build())
                    .queue();
            return;
        }

        msg.replyEmbeds(handleUnban(ctx.getGuild(), member.getUser()).build())
                .queue();
    }

    private EmbedBuilder handleUnban(Guild guild, User user) {
        final var guildConfig = new GuildConfig(guild);
        if (!guildConfig.isBannedUser(user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.USER_NOT_BANNED);

        guildConfig.unbanUser(user.getIdLong());

        user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.USER_UNBANNED, Pair.of("{server}", guild.getName())).build())
                .queue(success -> {}, new ErrorHandler()
                        .handle(
                                ErrorResponse.CANNOT_SEND_TO_USER,
                                e -> Listener.logger.warn("Was not able to send an unban message to " + user.getAsTag() + "(" + user.getIdLong() + ")")
                        )));

        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.UnbanMessages.USER_UNBANNED_RESPONSE, Pair.of("{user}", user.getAsMention()));
    }

    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public String getHelp(String prefix) {
        return "Unban a user from the bot\n\n" +
                "Usage: `"+ prefix +"unban <@user>`";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("unban")
                        .setDescription("Unban a user from the bot")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.USER,
                                        "user",
                                        "The user to unban",
                                        true
                                )
                        )
                        .checkForPermissions(Permission.ROBERTIFY_BAN)
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

        if (!predicateCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), BotConstants.getInsufficientPermsMessage(event.getGuild(), Permission.ROBERTIFY_BAN))
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var userToUnban = event.getOption("user").getAsUser();

        event.replyEmbeds(handleUnban(event.getGuild(), userToUnban).build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue();
    }
}
