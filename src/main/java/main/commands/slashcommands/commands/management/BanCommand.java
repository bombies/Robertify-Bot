package main.commands.slashcommands.commands.management;

import lombok.SneakyThrows;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.constants.Permission;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.guildconfig.GuildConfig;
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
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class BanCommand extends AbstractSlashCommand implements ICommand {
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
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a user to ban!").build())
                    .queue();
            return;
        }

        final var id = GeneralUtils.getDigitsOnly(args.get(0));

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user to ban\n" +
                            "Make sure to either **mention** the user, or provide their **ID**")
                            .setImage("https://i.imgur.com/1tMlhM2.png")
                            .build())
                    .queue();
            return;
        }

        final Member member;
        try {
            member = ctx.getGuild().retrieveMemberById(id).complete();
        } catch (ErrorResponseException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user to ban.").build())
                    .queue();
            return;
        }

        if (member == null) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid user to ban.").build())
                    .queue();
            return;
        }

        if (args.size() == 1) {
            msg.replyEmbeds(handleBan(ctx.getGuild(), member, ctx.getAuthor(), null).build())
                    .queue();
        } else {
            final var duration = args.get(1);
            msg.replyEmbeds(handleBan(ctx.getGuild(), member, ctx.getAuthor(), duration).build())
                    .queue();
        }
    }

    @SneakyThrows
    private EmbedBuilder handleBan(Guild guild, Member user, User mod, String duration) {
        if (duration != null)
            if (!GeneralUtils.isValidDuration(duration))
                return RobertifyEmbedUtils.embedMessage(guild, """
                        Invalid duration format.

                        *Example formats*: `1d`, `10s`, `5h`, `30m`""");

        Long bannedUntil = duration == null ? null : GeneralUtils.getFutureTime(duration);

        if (GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_ADMIN))
            return RobertifyEmbedUtils.embedMessage(guild, "You cannot ban an admin!");

        if (BotBDCache.getInstance().isDeveloper(user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, "You cannot ban a developer of Robertify!");

        if (new GuildConfig().isBannedUser(guild.getIdLong(), user.getIdLong()))
            return RobertifyEmbedUtils.embedMessage(guild, "This user is already banned.");

        if (bannedUntil == null) { // Perm ban
            new GuildConfig().banUser(guild.getIdLong(), user.getIdLong(), mod.getIdLong(), System.currentTimeMillis(), -1);

             user.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have been banned permanently in **"+guild.getName()+"**!")
                    .build())
                    .queue(success -> {}, new ErrorHandler()
                            .handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) ->
                                    Listener.logger.warn("Was not able to send an unban message to " + user.getUser().getAsTag() + "("+user.getIdLong()+")"))));
            return RobertifyEmbedUtils.embedMessage(guild, "You have banned " + user.getAsMention());
        } else {
            new GuildConfig().banUser(guild.getIdLong(), user.getIdLong(), mod.getIdLong(), System.currentTimeMillis(), bannedUntil);

            user.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have been banned for `"+GeneralUtils.formatDuration(duration)+"` in **"+guild.getName()+"**!")
                            .build())
                    .queue(success -> {}, new ErrorHandler()
                            .handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) ->
                                    Listener.logger.warn("Was not able to send an unban message to " + user.getUser().getAsTag() + "("+user.getIdLong()+")"))));

            Listener.scheduleUnban(guild, user.getUser());

            return RobertifyEmbedUtils.embedMessage(guild, "You have banned " + user.getAsMention() + " for `"+ GeneralUtils.formatDuration(duration)
                    +"`");
        }
    }

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public String getHelp(String prefix) {
        return "Ban a user from the bot\n\n" +
                "Usage: `"+ prefix +"ban <@user>`";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("ban")
                        .setDescription("Ban a user from the bot")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.USER,
                                        "user",
                                        "The user to unban",
                                        true
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "duration",
                                        "How long should the user be banned for",
                                        false
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
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), BotConstants.getInsufficientPermsMessage(Permission.ROBERTIFY_BAN))
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var userToBan = event.getOption("user").getAsMember();
        final var duration = event.getOption("duration") == null ? null : event.getOption("duration").getAsString();

        event.replyEmbeds(handleBan(event.getGuild(), userToBan, event.getUser(), duration).build())
                .setEphemeral(false)
                .queue();
    }
}
