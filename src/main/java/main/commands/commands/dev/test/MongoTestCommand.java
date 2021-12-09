package main.commands.commands.dev.test;

import main.commands.CommandContext;
import main.commands.commands.ITestCommand;
import main.utils.database.mongodb.MongoTestDB;
import net.dv8tion.jda.api.entities.Message;
import org.json.JSONObject;

import javax.script.ScriptException;
import java.util.List;

public class MongoTestCommand implements ITestCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();


        if (args.isEmpty()) {
            msg.reply("Provide args pls").queue();
            return;
        }

        MongoTestDB db = new MongoTestDB();

        switch (args.get(0).toLowerCase()) {
            case "add" -> {
                try {
                    db.addItem("304828928223084546", System.currentTimeMillis(), new JSONObject().put("test", "Test123"));
                    msg.addReaction("✅").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.addReaction("❌").queue();
                }
            }

            case "view" -> {
                try {
                    msg.reply(
                            "```json\n"+
                            db.getItemsString("guild_id", "304828928223084546")
                    + "```").queue();
                    msg.addReaction("✅").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.addReaction("❌").queue();
                }
            }

            case "remove" -> {
                try {
                    db.removeItem("304828928223084546");
                    msg.addReaction("✅").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.addReaction("❌").queue();
                }
            }
        }
    }

    @Override
    public String getName() {
        return "mongotest";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return ITestCommand.super.getAliases();
    }
}
