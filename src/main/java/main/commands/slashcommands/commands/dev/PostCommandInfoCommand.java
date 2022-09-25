package main.commands.slashcommands.commands.dev;

import main.commands.slashcommands.SlashCommandManager;
import main.main.Robertify;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public class PostCommandInfoCommand extends AbstractSlashCommand {
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
        Robertify.getRobertifyAPI().postCommandInfo(body);
        event.reply("Posted!").queue();
    }
}
