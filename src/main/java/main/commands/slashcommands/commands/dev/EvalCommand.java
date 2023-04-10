package main.commands.slashcommands.commands.dev;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

public class EvalCommand extends AbstractSlashCommand implements IDevCommand {
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
        if (!ctx.getAuthor().getId().equals(Config.get(ENV.OWNER_ID)))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        EmbedBuilder eb;

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
            engine.put("shards",  Robertify.getShardManager());
            engine.put("guild", ctx.getGuild());
            engine.put("member", ctx.getMember());
            engine.put("link", Robertify.getLavalink());
            engine.put("requester", WebUtils.ins);
            engine.put("http", new OkHttpClient());

            if (Robertify.getRobertifyAPI() != null)
                engine.put("robapi", Robertify.getRobertifyAPI());

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
    }

    @Override
    public String getName() {
        return "eval";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("eval")
                        .setDescription("Spooky scary evals...")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "src",
                                        "The source code to evaluate",
                                        true
                                )
                        )
                        .setDevCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!devCheck(event)) return;

        final var guild = event.getGuild();
        EmbedBuilder eb;
        final String src = event.getOption("src").getAsString();

        try {
            engine.put("event", event);
            engine.put("channel", event.getChannel().asGuildMessageChannel());
            engine.put("api", event.getJDA());
            engine.put("shards",  Robertify.getShardManager());
            engine.put("guild", event.getGuild());
            engine.put("member", event.getMember());
            engine.put("link", Robertify.getLavalink());
            engine.put("requester", WebUtils.ins);
            engine.put("http", new OkHttpClient());

            if (Robertify.getRobertifyAPI() != null)
                engine.put("robapi", Robertify.getRobertifyAPI());

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

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
}
