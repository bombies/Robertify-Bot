package main.commands.commands.management;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.commands.CommandContext;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.constants.Permission;
import main.constants.Toggles;
import main.constants.RobertifyEmoji;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.toggles.TogglesConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class TogglesCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(TogglesCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final User sender = ctx.getAuthor();
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(guild, sender, Permission.ROBERTIFY_ADMIN))
            return;

        GeneralUtils.setCustomEmbed(guild, "Toggles");

        var config = new TogglesConfig();
        if (args.isEmpty()) {
            var toggleIDs = new StringBuilder();
            var toggleNames = new StringBuilder();
            var toggleStatuses = new StringBuilder();

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");

            int toggleID = 0;
            for (Toggles toggle : Toggles.values()) {
                toggleIDs.append(++toggleID).append("\n");
                toggleNames.append(Toggles.parseToggle(toggle)).append("\n");
                toggleStatuses.append(config.getToggle(guild, toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
                        .append("\n");
            }

            eb.addField("Toggle ID", toggleIDs.toString(), true);
            eb.addField("Feature", toggleNames.toString(), true);
            eb.addField("Status", toggleStatuses.toString(), true);

            msg.replyEmbeds(eb.build()).queue();
        } else {
            var eb = new EmbedBuilder();
            switch (args.get(0).toLowerCase()) {
                case "restrictedvoice", "1", "rvc", "rvchannels" -> {
                    if (config.getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                        config.setToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled restricted voice channels **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled restricted voice channels **ON**");
                    }
                }
                case "restrictedtext", "2", "rtc", "rtchannels" -> {
                    if (config.getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                        config.setToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled restricted text channels **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled restricted text channels **ON**");
                    }
                }
                case "announcements", "3" -> {
                    if (config.getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) {
                        config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled announcing player messages **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled announcing player messages **ON**");
                    }
                }
                case "changelog", "4" -> {
                    if (config.getToggle(guild, Toggles.ANNOUNCE_CHANGELOGS)) {
                        config.setToggle(guild, Toggles.ANNOUNCE_CHANGELOGS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled announcing changelogs **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.ANNOUNCE_CHANGELOGS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled announcing changelogs **ON**");
                    }
                }
                case "globalannouncements", "5" -> {
                    if (config.getToggle(guild, Toggles.GLOBAL_ANNOUNCEMENTS)) {
                        config.setToggle(guild, Toggles.GLOBAL_ANNOUNCEMENTS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled global announcements **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.GLOBAL_ANNOUNCEMENTS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled global announcements **ON**");
                    }
                }
                case "requester", "6" -> {
                    if (config.getToggle(guild, Toggles.SHOW_REQUESTER)) {
                        config.setToggle(guild, Toggles.SHOW_REQUESTER, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled showing the requester in now playing messages **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.SHOW_REQUESTER, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled showing the requester in now playing messages **ON**");
                    }
                }
                case "8ball", "7" -> {
                    if (config.getToggle(guild, Toggles.EIGHT_BALL)) {
                        config.setToggle(guild, Toggles.EIGHT_BALL, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled the 8ball command **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.EIGHT_BALL, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled the 8ball command **ON**");
                    }
                }
                case "polls", "poll", "8" -> {
                    if (config.getToggle(guild, Toggles.POLLS)) {
                        config.setToggle(guild, Toggles.POLLS, false);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled the polls command **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.POLLS, true);
                        eb = RobertifyEmbedUtils.embedMessage(guild, "You have toggled the polls command **ON**");
                    }
                }
                case "dj" -> eb = handleDJToggles(guild, args);
                default -> eb = RobertifyEmbedUtils.embedMessage(guild, "Invalid toggle!");
            }
            msg.replyEmbeds(eb.build()).queue();
        }

        GeneralUtils.setDefaultEmbed(guild);
    }

    private EmbedBuilder handleDJToggles(Guild guild, List<String> args) {
        final CommandManager commandManager = new CommandManager(new EventWaiter());
        final TogglesConfig config = new TogglesConfig();

        if (args.size() < 2)
            return getDJTogglesEmbed(guild, commandManager, config);

        switch (args.get(1).toLowerCase()) {
            case "list" -> {
                return getDJTogglesEmbed(guild, commandManager, config);
            }
            default -> {
                ICommand command = commandManager.getCommand(args.get(1));

                if (command == null)
                    return RobertifyEmbedUtils.embedMessage(guild, "`"+args.get(1)+"` is an invalid command!");

                switch (Boolean.toString(config.getDJToggle(guild, command))) {
                    case "true" -> {
                        config.setDJToggle(guild, command, false);
                        return RobertifyEmbedUtils.embedMessage(guild, "You have toggled DJ only mode for the `"+command.getName()+"` command to: **OFF**");
                    }
                    case "false" -> {
                        config.setDJToggle(guild, command, true);
                        return RobertifyEmbedUtils.embedMessage(guild, "You have toggled DJ only mode for the `"+command.getName()+"` command to: **ON**");
                    }
                    default -> logger.error("Did not receive either true or false. Lol?? How??");
                }
            }
        }
        return null;
    }

    private EmbedBuilder getDJTogglesEmbed(Guild guild, CommandManager commandManager, TogglesConfig config) {
        var musicCmds = commandManager.getMusicCommands();
        var toggleNames = new StringBuilder();
        var toggleStatuses = new StringBuilder();

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");

        for (ICommand toggle : musicCmds) {
            toggleNames.append(toggle.getName()).append("\n");
            toggleStatuses.append(config.getDJToggle(guild, toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
                    .append("\n");
        }

        eb.addField("Command", toggleNames.toString(), true);
        eb.addBlankField(true);
        eb.addField("DJ Status", toggleStatuses.toString(), true);

        return eb;
    }

    @Override
    public String getName() {
        return "toggles";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Toggle specific features on or off!\n\n**__Usages__**\n" +
                "`"+ prefix +"toggles`\n" +
                "`"+ prefix +"toggles <toggle_name>`\n" +
                 "`"+ prefix +"toggles dj <list|command>`\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("t", "tog", "toggle");
    }
}
