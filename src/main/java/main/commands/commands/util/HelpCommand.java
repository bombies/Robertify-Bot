package main.commands.commands.util;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.CommandManager;
import main.commands.ICommand;
import main.commands.IDevCommand;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class HelpCommand implements ICommand {
    @SneakyThrows
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final String prefix = ServerUtils.getPrefix(ctx.getGuild().getIdLong());

        GeneralUtils.setCustomEmbed(
                BotConstants.ROBERTIFY_EMBED_TITLE + " | Help",
                "Type \"" + prefix + "help <command>\" to get more help on a specific command."
        );

        CommandManager manager = new CommandManager();

        if (args.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (ICommand cmd : manager.getCommands())
                stringBuilder.append("`").append(cmd.getName()).append("`, ");

            EmbedBuilder eb = EmbedUtils.embedMessage("**Prefix**: `" + prefix + "`");
            eb.addField("Commands", stringBuilder.toString(), false);
            msg.replyEmbeds(eb.build()).queue();

            GeneralUtils.setDefaultEmbed();
            return;
        } else if (args.get(0).equalsIgnoreCase("dev")) {
            if (!new BotUtils().isDeveloper(ctx.getAuthor().getId())) {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+args.get(0)+"`");
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed();
                return;
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (ICommand cmd : manager.getDevCommands())
                stringBuilder.append("`").append(cmd.getName()).append("`, ");

            EmbedBuilder eb = EmbedUtils.embedMessage("**Developer Commands**\n\n" +
                    "**Prefix**: `" + prefix + "`");
            eb.addField("Commands", stringBuilder.toString(), false);
            msg.replyEmbeds(eb.build()).queue();

            GeneralUtils.setDefaultEmbed();
            return;
        }

        String search = args.get(0);
        ICommand command = manager.getCommand(search);

        if (command == null) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+search+"`");
            msg.replyEmbeds(eb.build()).queue();
            GeneralUtils.setDefaultEmbed();
            return;
        } else if (command instanceof IDevCommand) {
            if (!new BotUtils().isDeveloper(ctx.getAuthor().getId())) {
                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing found for: `"+search+"`");
                msg.replyEmbeds(eb.build()).queue();
                GeneralUtils.setDefaultEmbed();
                return;
            }
        }

        EmbedBuilder eb = EmbedUtils.embedMessageWithTitle(
                BotConstants.ROBERTIFY_EMBED_TITLE + " | Help ["+command.getName()+"]",
                command.getHelp(msg.getGuild().getId())
        );
        msg.replyEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }
}
