package main.commands.commands.dev;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.BotDB;
import me.duncte123.botcommons.messaging.EmbedUtils;
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
        final BotDB botUtils = new BotDB();

        GeneralUtils.setCustomEmbed("Developer Tools", new Color(118, 0, 236));

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide arguments.\n\n" +
                    "**Valid args**: `add`, `remove`");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        switch(args.get(0).toLowerCase()) {
            case "add" -> add(botUtils, msg, args);
            case "remove" -> remove(botUtils, msg, args);
            default -> {
                EmbedBuilder eb = EmbedUtils.embedMessage("Invalid arguments.\n\n" +
                        "**Valid args**: `add`, `remove`");
                msg.replyEmbeds(eb.build()).queue();
            }
        }

        botUtils.closeConnection();
        GeneralUtils.setDefaultEmbed();
    }

    @SneakyThrows
    private void add(BotDB botUtils, Message msg, List<String> args) {
        if (args.size() <= 1) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide the ID of a user to add as a developer");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int names = 0;
        StringBuilder stringBuilder = new StringBuilder("Added ");

        for (String s : args.subList(1, args.size())) {
            if (!GeneralUtils.stringIsID(s)) {
                EmbedBuilder eb = EmbedUtils.embedMessage("`"+s+"` is an invalid ID.");
                msg.replyEmbeds(eb.build()).queue();
                continue;
            }

            User developer = Robertify.api.getUserById(s);

            if (developer == null)
                throw new NullPointerException("User with id "+s+" is null");

            if (botUtils.isDeveloper(s)) {
                EmbedBuilder eb = EmbedUtils.embedMessage("User with id `"+s+"` is already a developer");
                msg.replyEmbeds(eb.build()).queue();
                botUtils.createConnection();
                continue;
            }

            botUtils.createConnection();

            botUtils.addDeveloper(s);
            stringBuilder.append(developer.getAsMention())
                    .append(", ");
            names++;
        }

        if (names == 0) return;

        stringBuilder.append(args.size() > 2 ? "as developers" : "as a developer");

        EmbedBuilder eb = EmbedUtils.embedMessage(stringBuilder.toString());
        msg.replyEmbeds(eb.build()).queue();
    }

    @SneakyThrows
    private void remove(BotDB botUtils, Message msg, List<String> args) {
        if (args.size() <= 1) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide the ID of a user to remove as a developer");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int names = 0;
        StringBuilder stringBuilder = new StringBuilder("Removed ");

        for (String s : args.subList(1, args.size())) {
            if (!GeneralUtils.stringIsID(s)) {
                EmbedBuilder eb = EmbedUtils.embedMessage("`"+s+"` is an invalid ID.");
                msg.replyEmbeds(eb.build()).queue();
                continue;
            }

            User developer = Robertify.api.getUserById(s);

            if (developer == null)
                throw new NullPointerException("User with id "+s+" is null");

            if (!botUtils.isDeveloper(s)) {
                EmbedBuilder eb = EmbedUtils.embedMessage("User with id `"+s+"` is not a developer");
                msg.replyEmbeds(eb.build()).queue();
                botUtils.createConnection();
                continue;
            }

            botUtils.createConnection();

            botUtils.removeDeveloper(s);
            stringBuilder.append(developer.getAsMention())
                    .append(", ");
            names++;
        }

        if (names == 0) return;

        stringBuilder.append(args.size() > 2 ? "as developers" : "as a developer");

        EmbedBuilder eb = EmbedUtils.embedMessage(stringBuilder.toString());
        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "developer";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Developer command.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("dev", "devtools");
    }
}
