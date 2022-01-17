package main.commands.commands.management;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.main.Listener;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.json.guildconfig.GuildConfig;
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
                            "Make sure to either **mention** the channel, or provide its **ID**")
                            .setImage("https://i.imgur.com/1tMlhM2.png")
                            .build())
                    .queue();
            return;
        }

        final Member member = Robertify.api.getGuildById(ctx.getGuild().getIdLong()).retrieveMemberById(id).complete();

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

        if (BotInfoCache.getInstance().isDeveloper(user.getIdLong()))
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

        final var userToBan = event.getOption("user").getAsMember();
        final var duration = event.getOption("duration") == null ? null : event.getOption("duration").getAsString();

        event.replyEmbeds(handleBan(event.getGuild(), userToBan, event.getUser(), duration).build())
                .setEphemeral(false)
                .queue();
    }
}
