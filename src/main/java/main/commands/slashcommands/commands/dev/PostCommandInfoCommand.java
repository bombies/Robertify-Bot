package main.commands.slashcommands.commands.dev;

import main.commands.slashcommands.SlashCommandManager;
import main.main.Robertify;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import netscape.javascript.JSException;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PostCommandInfoCommand extends AbstractSlashCommand {
    final Logger logger = LoggerFactory.getLogger(PostCommandInfoCommand.class);

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("postcommandinfo")
                        .setDescription("Post all command info to the Robertify API!")
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

        SlashCommandManager slashCommandManager = new SlashCommandManager();
        final var commands = slashCommandManager.getCommands();

        final JSONObject body = new JSONObject();
        final JSONArray cmdsArr = new JSONArray();

        int id = 0;
        for (final var cmd : commands) {
            cmdsArr.put(new JSONObject()
                    .put("id", id++)
                    .put("name", cmd.getName())
                    .put("description", cmd.getDescription())
                    .put("category", slashCommandManager.getCommandType(cmd).name().toLowerCase())
            );
        }

        body.put("commands", cmdsArr);

        event.deferReply().queue();
        try (final var response = Robertify.getRobertifyAPI().postCommandInfo(body)) {
            if (response.code() == HttpStatus.SC_CREATED)
                event.getHook().sendMessage("Posted!")
                        .setEphemeral(true)
                        .queue();
            else {
                try {
                    final var errMsg = new JSONObject(response.body().string()).getString("message");

                    event.getHook().sendMessage("There was an issue attempting to post commands! Check console for more information.\n\nError Message: `"+errMsg+"`\nError Code: `"+response.code()+"`")
                            .setEphemeral(true)
                            .queue();
                    logger.error("There was an error when trying to POST commands: " + errMsg);
                } catch (JSONException e) {
                    event.getHook().sendMessage("There was an issue attempting to post commands! Check console for more information.\n\nError Code: `"+response.code()+"`")
                            .setEphemeral(true)
                            .queue();
                    logger.error(response.body().string());
                }

            }
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        }
    }
}
