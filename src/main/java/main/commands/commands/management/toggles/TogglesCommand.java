package main.commands.commands.management.toggles;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.commands.CommandContext;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.constants.RobertifyEmoji;
import main.utils.GeneralUtils;
import main.utils.database.ServerDB;
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

        GeneralUtils.setCustomEmbed("Toggles");

        if (args.isEmpty()) {
            var config = new TogglesConfig();
            var toggleNames = new StringBuilder();
            var toggleStatuses = new StringBuilder();

            EmbedBuilder eb = EmbedUtils.embedMessage("\t");

            for (Toggles toggle : Toggles.values()) {
                toggleNames.append(Toggles.parseToggle(toggle)).append("\n");
                toggleStatuses.append(config.getToggle(guild, toggle) ? RobertifyEmoji.CHECK_EMOJI.toString() : RobertifyEmoji.QUIT_EMOJI.toString())
                        .append("\n");
            }

            eb.addField("Feature", toggleNames.toString(), true);
            eb.addBlankField(true);
            eb.addField("Status", toggleStatuses.toString(), true);

            msg.replyEmbeds(eb.build()).queue();
        } else {
            var config = new TogglesConfig();
            var eb = new EmbedBuilder();
            switch (args.get(0).toLowerCase()) {
                case "announcements", "1" -> {
                    if (config.getToggle(guild, Toggles.ANNOUNCE_MESSAGES)) {
                        config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, false);
                        eb = EmbedUtils.embedMessage("You have toggled announcing player messages **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.ANNOUNCE_MESSAGES, true);
                        eb = EmbedUtils.embedMessage("You have toggled announcing player messages **ON**");
                    }
                }
                case "changelog", "2" -> {
                    if (config.getToggle(guild, Toggles.ANNOUNCE_CHANGELOGS)) {
                        config.setToggle(guild, Toggles.ANNOUNCE_CHANGELOGS, false);
                        eb = EmbedUtils.embedMessage("You have toggled announcing changelogs **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.ANNOUNCE_CHANGELOGS, true);
                        eb = EmbedUtils.embedMessage("You have toggled announcing changelogs **ON**");
                    }
                }
                case "requester", "3" -> {
                    if (config.getToggle(guild, Toggles.SHOW_REQUESTER)) {
                        config.setToggle(guild, Toggles.SHOW_REQUESTER, false);
                        eb = EmbedUtils.embedMessage("You have toggled showing the requester in now playing messages **OFF**");
                    } else {
                        config.setToggle(guild, Toggles.SHOW_REQUESTER, true);
                        eb = EmbedUtils.embedMessage("You have toggled showing the requester in now playing messages **ON**");
                    }
                }
                case "dj" -> eb = handleDJToggles(guild, args);
                default -> eb = EmbedUtils.embedMessage("Invalid toggle!");
            }
            msg.replyEmbeds(eb.build()).queue();
        }

        GeneralUtils.setDefaultEmbed();
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
                    return EmbedUtils.embedMessage("`"+args.get(1)+"` is an invalid command!");

                switch (Boolean.toString(config.getDJToggle(guild, command))) {
                    case "true" -> {
                        config.setDJToggle(guild, command, false);
                        return EmbedUtils.embedMessage("You have toggled DJ only mode for the `"+command.getName()+"` command to: **OFF**");
                    }
                    case "false" -> {
                        config.setDJToggle(guild, command, true);
                        return EmbedUtils.embedMessage("You have toggled DJ only mode for the `"+command.getName()+"` command to: **ON**");
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

        EmbedBuilder eb = EmbedUtils.embedMessage("\t");

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
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Toggle specific features on or off!\n\n**__Usages__**\n" +
                "`"+ ServerDB.getPrefix(Long.parseLong(guildID)) +"toggles`\n" +
                "`"+ ServerDB.getPrefix(Long.parseLong(guildID)) +"toggles <toggle_name>`\n" +
                 "`"+ ServerDB.getPrefix(Long.parseLong(guildID)) +"toggles dj <list|command>`\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("t", "tog", "toggle");
    }
}
