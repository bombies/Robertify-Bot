package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.constants.RobertifyEmoji;
import main.constants.TimeFormat;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.changelog.ChangeLogConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.themes.ThemesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChangeLogCommand implements IDevCommand {
    private static final Logger logger = LoggerFactory.getLogger(ChangeLogCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed(ctx.getGuild(), "Change Log");

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide arguments");
            msg.replyEmbeds(eb.build()).queue();
        } else {
            switch (args.get(0).toLowerCase()) {
                case "send" -> send(msg);
                case "view" -> view(msg);
                case "remove" -> remove(args, msg);
                case "clear" -> clear(msg);
                case "settitle", "st" -> setTitle(args, msg);
                case "addfeatue", "af" -> add(ChangeLogConfig.LogType.FEATURE, args, msg);
                case "addbugfix", "abf" -> add(ChangeLogConfig.LogType.BUGFIX, args, msg);
            }
        }

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    private void send(Message msg) {
        final var config = new ChangeLogConfig();
        final var guilds = Robertify.api.getGuilds();

        final ExecutorService executorService = Executors.newCachedThreadPool();

        for (Guild g : guilds) {
            TextChannel announcementChannel = Robertify.api.getTextChannelById(new GuildConfig().getAnnouncementChannelID(g.getIdLong()));

            if (announcementChannel == null) continue;

            try {
                executorService.execute(() -> announcementChannel.sendMessageEmbeds(getLogEmbed(config.getTitle(), g, false)).queueAfter(1, TimeUnit.SECONDS, null, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> logger.error("Was not able to send a changelog in {}", g.getName()))));
            } catch (InsufficientPermissionException e) {
                logger.error("Was not able to send a changelog in {}", g.getName());
            }
        }

        config.sendLog();
        msg.addReaction("✅").queue();
    }

    private void view(Message msg) {
        final var config = new ChangeLogConfig();
        final var guild = msg.getGuild();

        msg.replyEmbeds(getLogEmbed(config.getTitle(), guild, true)).queue();
    }

    private void remove(List<String> args, Message msg) {
        final var guild = msg.getGuild();

        if (args.size() < 2) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a changelog to remove");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int id;
        if (GeneralUtils.stringIsInt(args.get(1))) {
            id = Integer.parseInt(args.get(1));
        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid integer as an ID!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        var config = new ChangeLogConfig();

        if (id < 0 || id > config.getCurrentChangelog().size()-1) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "The ID cannot be less than 0 or more than the current amount of logs");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        config.removeChangeLog(id);
        msg.addReaction("✅").queue();
    }

    private void add(ChangeLogConfig.LogType type, List<String> args, Message msg) {
        if (args.size() < 2) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(msg.getGuild(), "You must provide a changelog to add");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        var config = new ChangeLogConfig();
        var log = GeneralUtils.getJoinedString(args, 1).replaceAll("\\\\n", "\n");
        config.addChangeLog(type, log);
        msg.addReaction("✅").queue();
    }

    private void setTitle(List<String> args, Message msg) {
        if (args.size() < 2) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(msg.getGuild(), "You must provide a title");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        var config = new ChangeLogConfig();
        var title = GeneralUtils.getJoinedString(args, 1).replaceAll("\\\\n", "\n");
        config.setTitle(title);
        msg.addReaction("✅").queue();
    }

    private void clear(Message msg) {
        new ChangeLogConfig().clearChangeLog();
        msg.addReaction("✅").queue();
    }

    private MessageEmbed getLogEmbed(String title, Guild guild, boolean devView) {
        final var config = new ChangeLogConfig();
        final var logs = config.getCurrentChangelog();
        final var features = new StringBuilder();
        final var bugFixes = new StringBuilder();

        int i = 0;
        for (var log : logs) {
            switch (log.getLeft()) {
                case FEATURE -> features.append(RobertifyEmoji.FEATURE).append(" ").append(log.getRight()).append(devView ? " ("+(i++)+")" : "").append("\n\n");
                case BUGFIX -> bugFixes.append(RobertifyEmoji.BUG_FIX).append(" ").append(log.getRight()).append(devView ? " ("+(i++)+")"  : "").append("\n\n");
            }
        }

        return RobertifyEmbedUtils.embedMessage(guild, "\t")
                .setTitle("✨ " + title + " ["+GeneralUtils.formatDate(new Date().getTime(), TimeFormat.MM_DD_YYYY)+"]")
                .setDescription("➕ **Features**\n" +  features + "\n🛠️ **Bug Fixes**\n" + bugFixes)
                .setFooter("Note: You can toggle changelogs for this server off by doing \"toggle changelogs\"")
                .setThumbnail(new ThemesConfig().getTheme(guild.getIdLong()).getTransparent())
                .build();
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
