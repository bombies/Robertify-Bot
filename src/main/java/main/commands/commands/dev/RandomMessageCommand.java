package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.commands.RandomMessageManager;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class RandomMessageCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.reply("Please provide args").queue();
        } else {
            switch (args.get(0).toLowerCase()) {
                case "add" -> add(msg, args);
                case "remove" -> remove(msg, args);
                case "list" -> list(msg);
                case "clear" -> clear(msg);
                default -> msg.reply("Invalid args").queue();
            }
        }
    }

    public void add(Message msg, List<String> args) {
        if (args.size() < 2) {
            msg.reply("Please provide a message to add lol").queue();
            return;
        }

        final String message = String.join(" ", args.subList(1, args.size()))
                .replaceAll("\\\\n", "\n");

        new RandomMessageManager().addMessage(message);
        msg.addReaction("✅").queue();
    }

    public void remove(Message msg, List<String> args) {
        if (args.size() < 2) {
            msg.reply("Please provide an ID to remove").queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(1))) {
            msg.reply("Invalid ID!").queue();
            return;
        }

        int id = Integer.parseInt(args.get(1));

        try {
            new RandomMessageManager().removeMessage(id);
            msg.addReaction("✅").queue();
        } catch (IndexOutOfBoundsException e) {
            msg.reply(e.getMessage()).queue();
        }
    }

    public void list(Message msg) {
        final var messages = new RandomMessageManager().getMessages();
        final var sb = new StringBuilder();
        sb.append("```\n");
        for (int i = 0; i < messages.size(); i++)
            sb.append(i).append(" - ").append("*").append(messages.get(i)).append("*\n");
        sb.append("```");
        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(msg.getGuild(), sb.toString()).build()).queue();
    }

    public void clear(Message msg) {
        new RandomMessageManager().clearMessages();
        msg.reply("✅").queue();
    }

    @Override
    public String getName() {
        return "randommessage";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("rm");
    }
}
