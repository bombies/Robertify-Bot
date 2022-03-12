package main.commands.prefixcommands.dev.test;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ITestCommand;
import main.utils.database.mongodb.databases.MongoTestDB;
import main.utils.database.mongodb.cache.TestMongoCache;
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

        MongoTestDB db = MongoTestDB.ins();

        switch (args.get(0).toLowerCase()) {
            case "add" -> {
                try {
                    db.addItem(ctx.getGuild().getId(), System.currentTimeMillis(), new JSONObject().put("test", "Test123"));
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
                            db.getItemString("guild_id", ctx.getGuild().getId(), true)
                    + "```").queue();
                    msg.addReaction("✅").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.addReaction("❌").queue();
                }
            }

            case "remove" -> {
                try {
                    db.removeItem(ctx.getGuild().getId());
                    msg.addReaction("✅").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.addReaction("❌").queue();
                }
            }

            case "update" -> {
                try {
                    db.updateItem(ctx.getGuild().getId(), "time", "ballslol");
                    msg.addReaction("✅").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.addReaction("❌").queue();
                }
            }

            case "cache" -> {
                try {
                    msg.reply("```json\n" + TestMongoCache.getInstance().cacheTest() + "```").queue();
                    msg.addReaction("✅").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.addReaction("❌").queue();
                }
            }

            default -> msg.addReaction("❓").queue();
        }
    }

    @Override
    public String getName() {
        return "mongotest";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return ITestCommand.super.getAliases();
    }
}
