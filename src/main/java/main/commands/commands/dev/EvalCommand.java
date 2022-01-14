package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.util.List;

public class EvalCommand implements IDevCommand {
    private final Logger logger = LoggerFactory.getLogger(EvalCommand.class);
    private final ScriptEngine engine;

    public EvalCommand() {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("var imports = new JavaImporter(" +
                    "java.io," +
                    "java.lang," +
                    "java.util," +
                    "Packages.net.dv8tion.jda.api," +
                    "Packages.net.dv8tion.jda.api.entities," +
                    "Packages.net.dv8tion.jda.api.entities.impl," +
                    "Packages.net.dv8tion.jda.api.managers," +
                    "Packages.net.dv8tion.jda.api.managers.impl," +
                    "Packages.net.dv8tion.jda.api.utils);");
        } catch (ScriptException e) {
            logger.error("[FATAL ERROR] Script error occurred!", e);
        }
    }

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!ctx.getAuthor().getId().equals("274681651945144321"))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        EmbedBuilder eb;
        GeneralUtils.setCustomEmbed(guild, new Color(0, 183, 255));

        if (args.isEmpty()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a snippet to evaluate!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final String src = String.join(" ", args);

        try {
            engine.put("event", ctx.getEvent());
            engine.put("message", ctx.getMessage());
            engine.put("channel", ctx.getChannel());
            engine.put("args", args);
            engine.put("api", ctx.getJDA());
            engine.put("guild", ctx.getGuild());
            engine.put("member", ctx.getMember());

            Object out = engine.eval(
                    "(function() {\n" +
                            " with (imports) { \n" +
                            src +
                            " \n}" +
                            "\n})();");

            if (out != null) {
                eb = RobertifyEmbedUtils.embedMessage(guild, "```java\n" + src + "```");
                eb.addField("Result", out.toString(), false);
            } else
                eb = RobertifyEmbedUtils.embedMessage(guild, "```java\nExecuted without error.```");

        } catch (Exception e) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "```java\n" + e.getMessage() +"```");
        }

        msg.replyEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    @Override
    public String getName() {
        return "eval";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
