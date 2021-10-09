package main.commands.commands.management.toggles;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.utils.GeneralUtils;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.List;

public class TogglesCommand implements ICommand {
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
            var toggleStatuses = new StringBuilder();

            for (Toggles toggle : Toggles.values())
                toggleStatuses.append("→ **").append(Toggles.parseToggle(toggle)).append("** — ").append(config.getToggle(guild, toggle) ? "✅" : "❌").append("\n");

            var eb = EmbedUtils.embedMessage(toggleStatuses.toString());
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
                default -> eb = EmbedUtils.embedMessage("Invalid toggle!");
            }
            msg.replyEmbeds(eb.build()).queue();
        }

        GeneralUtils.setDefaultEmbed();
    }

    @Override
    public String getName() {
        return "toggles";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Toggle specific features on or off!\n\n" +
                "Usage: `"+ ServerUtils.getPrefix(Long.parseLong(guildID)) +"toggles\n`" +
                "Usage: `"+ ServerUtils.getPrefix(Long.parseLong(guildID)) +"toggle <toggle_name>\n`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("t", "tog", "toggle");
    }
}
