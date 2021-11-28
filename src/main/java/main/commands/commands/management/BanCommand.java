package main.commands.commands.management;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.main.Listener;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.BanDB;
import main.utils.database.BotDB;
import main.utils.database.ServerDB;
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

public class BanCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_ADMIN)) {
            ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage("You do not have permission to run this command!")
                    .build())
                    .queue();
            return;
        }

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide a user to ban!").build())
                    .queue();
            return;
        }

        final var id = GeneralUtils.getDigitsOnly(args.get(0));

        if (!GeneralUtils.stringIsID(id)) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide a valid user to ban.").build())
                    .queue();
            return;
        }

        final Member member = Robertify.api.getGuildById(ctx.getGuild().getIdLong()).getMemberById(id);

        if (member == null) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide a valid user to ban.").build())
                    .queue();
            return;
        }

        if (args.size() == 1) {
            msg.replyEmbeds(handleBan(ctx.getGuild(), member.getUser(), ctx.getAuthor(), null).build())
                    .queue();
        } else {
            final var duration = args.get(1);
            msg.replyEmbeds(handleBan(ctx.getGuild(), member.getUser(), ctx.getAuthor(), duration).build())
                    .queue();
        }
    }

    @SneakyThrows
    private EmbedBuilder handleBan(Guild guild, User user, User mod, String duration) {
        if (duration != null)
            if (!GeneralUtils.isValidDuration(duration))
                return EmbedUtils.embedMessage("Invalid duration format.\n\n" +
                        "*Example formats*: `1d`, `10s`, `5h`, `30m`");

        Long bannedUntil = duration == null ? null : GeneralUtils.getFutureTime(duration);

        if (GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_ADMIN))
            return EmbedUtils.embedMessage("You cannot ban another admin!");

        if (new BotDB().isDeveloper(user.getId()))
            return EmbedUtils.embedMessage("You cannot ban a developer of Robertify!");

        if (BanDB.isUserBannedLazy(guild.getIdLong(), user.getIdLong()))
            return EmbedUtils.embedMessage("This user is already banned.");

        if (bannedUntil == null) { // Perm ban
            new BanDB().banUser(guild.getIdLong(), user.getIdLong(), mod.getIdLong(), System.currentTimeMillis());
            BanDB.addBannedUser(guild.getIdLong(), user.getIdLong(), null);
            user.openPrivateChannel().queue(channel -> {
                channel.sendMessageEmbeds(EmbedUtils.embedMessage("You have been banned permanently in **"+guild.getName()+"**!")
                        .build())
                        .queue(success -> {}, new ErrorHandler()
                                .handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) ->
                                        Listener.LOGGER.warn("Was not able to send an unban message to " + user.getAsTag() + "("+user.getIdLong()+")")));
            });
            return EmbedUtils.embedMessage("You have banned " + user.getAsMention());
        } else {
            new BanDB().banUser(guild.getIdLong(), user.getIdLong(), mod.getIdLong(), System.currentTimeMillis(), bannedUntil);
            BanDB.addBannedUser(guild.getIdLong(), user.getIdLong(), bannedUntil);
            user.openPrivateChannel().queue(channel -> {
                channel.sendMessageEmbeds(EmbedUtils.embedMessage("You have been banned for `"+GeneralUtils.formatDuration(duration)+"` in **"+guild.getName()+"**!")
                                .build())
                        .queue(success -> {}, new ErrorHandler()
                                .handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) ->
                                        Listener.LOGGER.warn("Was not able to send an unban message to " + user.getAsTag() + "("+user.getIdLong()+")")));
            });

            Listener.scheduleUnban(guild, user);

            return EmbedUtils.embedMessage("You have banned " + user.getAsMention() + " for `"+ GeneralUtils.formatDuration(duration)
                    +"`");
        }
    }

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`" +
                "\nBan a user from the bot\n\n" +
                "Usage: `"+ ServerDB.getPrefix(Long.parseLong(guildID)) +"ban <user>`";
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
                        "Ban a user from the bot",
                        List.of(CommandOption.of(
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
                        ),
                        e -> GeneralUtils.hasPerms(e.getGuild(), e.getUser(), Permission.ROBERTIFY_ADMIN),
                        true
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        if (!getInteractionCommand().getCommand().permissionCheck(event)) {
            event.replyEmbeds(EmbedUtils.embedMessage("You do not have permission to execute this command!")
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var userToBan = event.getOption("user").getAsUser();
        final var duration = event.getOption("duration") == null ? null : event.getOption("duration").getAsString();

        event.replyEmbeds(handleBan(event.getGuild(), userToBan, event.getUser(), duration).build())
                .setEphemeral(false)
                .queue();
    }
}
