package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.constants.BotConstants;
import main.constants.TimeFormat;
import main.exceptions.NoEmbedPermissionException;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.BotDB;
import main.utils.json.changelog.ChangeLogConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChangeLogCommand implements IDevCommand {
    private static final Logger logger = LoggerFactory.getLogger(ChangeLogCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        GeneralUtils.setCustomEmbed("Change Log");

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide arguments");
            msg.replyEmbeds(eb.build()).queue();
        } else {
            switch (args.get(0).toLowerCase()) {
                case "send" -> send(msg);
                case "view" -> view(msg);
                case "remove" -> remove(args, msg);
                case "add" -> add(args, msg);
            }
        }

        GeneralUtils.setDefaultEmbed();
    }

    private void send(Message msg) {
        var config = new ChangeLogConfig();
        var botUtils = new BotDB();
        var logsToString = new StringBuilder();
        var logs = config.getCurrentChangelog();
        var guilds = botUtils.getGuilds();

        botUtils.createConnection();

        logs.forEach(log -> logsToString.append("**—** ").append(log).append("\n\n"));
        EmbedBuilder eb = EmbedUtils.embedMessage(logsToString.toString());
        eb.setThumbnail(BotConstants.ROBERTIFY_CHRISTMAS_LOGO_TRANSPARENT.toString());
        eb.setFooter("Note: You can toggle changelogs for this server off by doing \"toggle changelogs\"");

        eb.setTitle("["+GeneralUtils.formatDate(new Date().getTime(), TimeFormat.MM_DD_YYYY)+"]");

        for (Guild g : guilds) {
            if (!new TogglesConfig().getToggle(msg.getGuild(), Toggles.ANNOUNCE_CHANGELOGS))
                continue;

            botUtils.createConnection();
            TextChannel announcementChannel = botUtils.getAnnouncementChannelObject(g.getIdLong());

            if (announcementChannel == null) continue;

            try {
                announcementChannel.sendMessageEmbeds(eb.build()).queueAfter(1, TimeUnit.SECONDS, null, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            logger.error("Was not able to send a changelog in {}", g.getName());
                        }));
            } catch (InsufficientPermissionException e) {
                logger.error("Was not able to send a changelog in {}", g.getName());
            }
        }

        config.sendLog();
        msg.addReaction("✅").queue();
    }

    private void view(Message msg) {
        var config = new ChangeLogConfig();
        var logs = config.getCurrentChangelog();
        var logsToString = new StringBuilder();

        for (int i = 0; i < logs.size(); i++)
            logsToString.append("**—** ").append(logs.get(i)).append(" *(").append(i).append(")*\n\n");

        EmbedBuilder eb = EmbedUtils.embedMessage(logsToString.toString());
        eb.setThumbnail(BotConstants.ROBERTIFY_CHRISTMAS_LOGO_TRANSPARENT.toString());
        eb.setFooter("Note: You can toggle changelogs for this server off by doing \"toggle changelogs\"");

        eb.setTitle("["+GeneralUtils.formatDate(new Date().getTime(), TimeFormat.MM_DD_YYYY)+"]");

        msg.replyEmbeds(eb.build()).queue();
    }

    private void remove(List<String> args, Message msg) {
        if (args.size() < 2) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide the ID of a changelog to remove");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int id;
        if (GeneralUtils.stringIsInt(args.get(1))) {
            id = Integer.parseInt(args.get(1));
        } else {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid integer as an ID!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        var config = new ChangeLogConfig();

        if (id < 0 || id > config.getCurrentChangelog().size()-1) {
            EmbedBuilder eb = EmbedUtils.embedMessage("The ID cannot be less than 0 or more than the current amount of logs");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        config.removeChangeLog(id);
        msg.addReaction("✅").queue();
    }

    private void add(List<String> args, Message msg) {
        if (args.size() < 2) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a changelog to add");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        var config = new ChangeLogConfig();
        var log = GeneralUtils.getJoinedString(args, 1);
        config.addChangeLog(log);
        msg.addReaction("✅").queue();
    }

    @Override
    public String getName() {
        return "changelog";
    }

    @Override
    public String getHelp(String prefix) {
        return "Send out changelogs for the bot!";
    }
}
