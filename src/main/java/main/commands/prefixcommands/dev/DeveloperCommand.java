package main.commands.prefixcommands.dev;

import lombok.SneakyThrows;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.cache.BotBDCache;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.awt.*;
import java.util.List;

public class DeveloperCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (ctx.getAuthor().getIdLong() != 274681651945144321L) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var botUtils = BotBDCache.getInstance();
        final var guild = ctx.getGuild();

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, """
                    You must provide arguments.

                    **Valid args**: `add`, `remove`""");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        switch(args.get(0).toLowerCase()) {
            case "add" -> add(botUtils, msg, args);
            case "remove" -> remove(botUtils, msg, args);
            default -> {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, """
                        Invalid arguments.

                        **Valid args**: `add`, `remove`""");
                msg.replyEmbeds(eb.build()).queue();
            }
        }
    }

    @SneakyThrows
    private void add(BotBDCache botUtils, Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() <= 1) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a user to add as a developer");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int names = 0;
        StringBuilder stringBuilder = new StringBuilder("Added ");

        for (String s : args.subList(1, args.size())) {
            if (!GeneralUtils.stringIsID(s)) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "`"+s+"` is an invalid ID.");
                msg.replyEmbeds(eb.build()).queue();
                continue;
            }

            User developer = GeneralUtils.retrieveUser(s);

            if (developer == null)
                throw new NullPointerException("User with id "+s+" is null");

            if (botUtils.isDeveloper(Long.parseLong(s))) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "User with id `"+s+"` is already a developer");
                msg.replyEmbeds(eb.build()).queue();
                continue;
            }

            botUtils.addDeveloper(Long.parseLong(s));
            stringBuilder.append(developer.getAsMention())
                    .append(", ");
            names++;
        }

        if (names == 0) return;

        stringBuilder.append(args.size() > 2 ? "as developers" : "as a developer");

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, stringBuilder.toString());
        msg.replyEmbeds(eb.build()).queue();
    }

    @SneakyThrows
    private void remove(BotBDCache botUtils, Message msg, List<String> args) {
        final var guild = msg.getGuild();

        if (args.size() <= 1) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a user to remove as a developer");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int names = 0;
        StringBuilder stringBuilder = new StringBuilder("Removed ");

        for (String s : args.subList(1, args.size())) {
            if (!GeneralUtils.stringIsID(s)) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "`"+s+"` is an invalid ID.");
                msg.replyEmbeds(eb.build()).queue();
                continue;
            }

            User developer = GeneralUtils.retrieveUser(s);

            if (developer == null)
                throw new NullPointerException("User with id "+s+" is null");

            if (!botUtils.isDeveloper(Long.parseLong(s))) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "User with id `"+s+"` is not a developer");
                msg.replyEmbeds(eb.build()).queue();
                continue;
            }

            botUtils.removeDeveloper(Long.parseLong(s));
            stringBuilder.append(developer.getAsMention())
                    .append(", ");
            names++;
        }

        if (names == 0) return;

        stringBuilder.append(args.size() > 2 ? "as developers" : "as a developer");

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, stringBuilder.toString());
        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "developer";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Developer command.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("dev", "devtools");
    }
}
