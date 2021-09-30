//package main.commands.commands.dev.database;
//
//import main.commands.CommandContext;
//import main.commands.ICommand;
//import main.commands.ITestCommand;
//import main.constants.DatabaseTable;
//import main.utils.GeneralUtils;
//import main.utils.database.DatabaseUtils;
//import main.utils.database.ServerUtils;
//import me.duncte123.botcommons.messaging.EmbedUtils;
//import net.dv8tion.jda.api.EmbedBuilder;
//import net.dv8tion.jda.api.entities.Guild;
//import net.dv8tion.jda.api.entities.Message;
//
//import javax.script.ScriptException;
//import java.awt.*;
//import java.sql.SQLException;
//import java.util.List;
//
//public class SQLCommand implements ITestCommand {
//    @Override
//    public void handle(CommandContext ctx) throws ScriptException {
//        if (permissionCheck(ctx)) {
//            final List<String> args = ctx.getArgs();
//            final Guild guild = ctx.getGuild();
//            final Message msg = ctx.getMessage();
//            EmbedBuilder eb = new EmbedBuilder();
//            GeneralUtils.setCustomEmbed("Robertify | SQL Control", new Color(26, 139, 1));
//
//            if (args.isEmpty()) {
//                String prefix = ServerUtils.getPrefix(guild.getIdLong());
//
//                eb = EmbedUtils.embedMessage("You must provide arguments!\n" +
//                        "\t- `"+ prefix+"sql listtables`\n"
//                        + "\t- `"+prefix+"sql viewtable <tablename>`\n"
//                        + "\t- `"+prefix+"sql update <sql>`\n"
//                        + "\t- `"+prefix+"sql base <sql>`\n");
//                msg.replyEmbeds(eb.build()).queue();
//            } else {
//                switch (args.get(0).toLowerCase()) {
//                    case "listtables", "lt" -> listTables(msg, eb);
//                    case "viewtable", "vt" -> viewTable(guild, msg, args, eb);
//                    case "update" -> update(msg, args);
//                    case "base" -> base(msg, args);
//                    default -> {
//                        eb = EmbedUtils.embedMessage("You must provide a valid argument!\n" +
//                                "Args: `listtable`, `viewtable`, `update`");
//                        msg.replyEmbeds(eb.build()).queue();
//                    }
//                }
//            }
//        }
//        GeneralUtils.setDefaultEmbed();
//    }
//
//    private void listTables(Message msg, EmbedBuilder eb) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("**Table Names**\n");
//        for (String s : DatabaseTable.)
//            sb.append("- ").append(s).append("\n");
//        eb = EmbedUtils.embedMessage(sb.toString());
//        msg.replyEmbeds(eb.build()).queue();
//    }
//
//    private void viewTable(Guild guild, Message msg, List<String> args, EmbedBuilder eb) {
//        if (args.size() >= 2) {
//            try {
//                List<String> columnNames = DatabaseUtils.getColumnHeadings(args.get(1));
//                List<List<String>> info = DatabaseUtils.getAllTableInfo(args.get(1));
//                if (info.isEmpty()) {
//                    eb = EmbedUtils.embedMessage("There is no information in this table!");
//                    msg.replyEmbeds(eb.build()).queue();
//                } else {
//                    StringBuilder sb = new StringBuilder();
//                    sb.append("```");
//                    for (String s : columnNames)
//                        sb.append("**" + s + "**\t");
//                    sb.append("\n");
//                    for (int i = 0; i < info.size(); i++) {
//                        for (int j = 0; j < info.get(i).size(); j++)
//                            sb.append(info.get(i).get(j)).append("\t");
//                        sb.append("\n");
//                    }
//                    sb.append("```");
//                    msg.reply(sb.toString()).queue();
//                }
//            } catch (SQLException e) {
//                eb = EmbedUtils.embedMessage("SQL Error somewhere!\n" +
//                        "More than likely that table name doesn't exist.");
//                msg.replyEmbeds(eb.build()).queue();
//            }
//        } else {
//            eb = EmbedUtils.embedMessage("You must provide a valid table name!\n" +
//                    "\t- "+ServerUtils.getPrefix(guild.getIdLong())+"sql viewtable <tablename>");
//            msg.replyEmbeds(eb.build()).queue();
//        }
//    }
//
//    private void update(Message msg, List<String> args) {
//        EmbedBuilder eb;
//        if (args.size() >= 3) {
//            String sql = GeneralUtils.getJoinedString(args, 1, args.size());
//            try {
//                DatabaseUtils.executeAnyUpdate(sql);
//                msg.addReaction("✅").queue();
//            } catch (SQLException e) {
//                eb = EmbedUtils.embedMessage("❌ **Error!**\n\n" + e.getMessage());
//                msg.replyEmbeds(eb.build()).queue();
//            }
//        } else {
//            eb = EmbedUtils.embedMessage("You ust provide a valid SQL query!");
//            msg.replyEmbeds(eb.build()).queue();
//        }
//    }
//
//    private void base(Message msg, List<String> args) {
//        EmbedBuilder eb;
//        if (args.size() >= 3) {
//            String sql = GeneralUtils.getJoinedString(args, 1, args.size());
//            try {
//                DatabaseUtils.executeAnyBase(sql);
//                msg.addReaction("✅").queue();
//            } catch (SQLException e) {
//                eb = EmbedUtils.embedMessage("❌ **Error!**\n\n" + e.getMessage());
//                msg.replyEmbeds(eb.build()).queue();
//            }
//        } else {
//            eb = EmbedUtils.embedMessage("You ust provide a valid SQL query!");
//            msg.replyEmbeds(eb.build()).queue();
//        }
//    }
//
//
//    @Override
//    public String getName() {
//        return "sql";
//    }
//
//    @Override
//    public String getHelp(String guildID) {
//        return "Manage the database from here!\n" +
//                "**Usages**:\n" +
//                "\t- `"+ServerUtils.getPrefix(guildID)+"sql listtables`\n"
//                + "\t- `"+ServerUtils.getPrefix(guildID)+"sql viewtable <tablename>`\n"
//                + "\t- `"+ServerUtils.getPrefix(guildID)+"sql update <sql>`\n";
//    }
//}
