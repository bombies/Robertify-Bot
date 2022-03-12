package main.commands.prefixcommands.dev;

import lombok.SneakyThrows;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.constants.ENV;
import main.constants.JSONConfigFile;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.database.mongodb.cache.GuildsDBCache;
import net.dv8tion.jda.api.entities.Message;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ViewConfigCommand implements IDevCommand {
    final Logger logger = LoggerFactory.getLogger(ViewConfigCommand.class);

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.reply("You must provide the name of a config file!").queue();
        } else {
            String fileName = args.get(0);

            if (GeneralUtils.stringIsID(fileName)) {
                JSONObject guildInfo = GuildsDBCache.getInstance().getGuildInfo(Long.parseLong(fileName));

                if (guildInfo == null) {
                    msg.reply("Invalid guild ID").queue();
                    return;
                }

                try {
                    msg.reply("```json\n" +
                            guildInfo.toString(4) + "\n```").queue();
                } catch (IllegalArgumentException e) {
                    File file = new File(fileName + ".json");
                    file.createNewFile();
                    ctx.getMessage().reply(file).queue(success -> file.delete());
                }

            } else {

                if (Arrays.stream(JSONConfigFile.values()).anyMatch(
                        (it) -> it.toString().equalsIgnoreCase(fileName)
                )) {

                    String info = GeneralUtils.getFileContent(Config.get(ENV.JSON_DIR) + "/" + fileName);

                    try {
                        msg.reply("```json\n" +
                                info + "\n```").queue();
                    } catch (IllegalArgumentException e) {
                        File file = new File(Config.get(ENV.JSON_DIR) + "/" + fileName.toLowerCase());
                        ctx.getMessage().reply(file).queue();
                    }

                } else msg.reply("Invalid config file!").queue();
            }
        }
    }

    @Override
    public String getName() {
        return "viewconfig";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "View the JSON configurations associated with the bot\n" +
                "\nUsage: `"+ prefix+"viewconfig <filename>.json`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("vconf");
    }
}
