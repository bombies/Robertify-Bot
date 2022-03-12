package main.commands.prefixcommands.dev;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.constants.Statistic;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.databases.StatisticsDB;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.List;

public class StatisticsCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var args = ctx.getArgs();
        final var msg = ctx.getMessage();

        if (args.isEmpty()) {
            getStats(msg, StatisticsDB.TimeFormat.DAY);
        } else {
            switch (args.get(0).toLowerCase()) {
                case "day" -> getStats(msg, StatisticsDB.TimeFormat.DAY);
                case "week" -> getStats(msg, StatisticsDB.TimeFormat.WEEK);
                case "month" -> getStats(msg, StatisticsDB.TimeFormat.MONTH);
                case "year" -> getStats(msg, StatisticsDB.TimeFormat.YEAR);
                default -> msg.reply("Invalid args").queue();
            }
        }
    }

    public void getStats(Message msg, StatisticsDB.TimeFormat format) {
        final var db = StatisticsDB.INSTANCE;
        final var commandsUsed = db.getCurrentStatistic(Statistic.COMMANDS_USED, format);

        String periodStr = null;

        switch (format) {
            case DAY -> periodStr = "Today";
            case WEEK -> periodStr = "This Week";
            case MONTH -> periodStr = "This Month";
            case YEAR -> periodStr = "This Year";
        }

        msg.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(msg.getGuild(),
                        "Stats For " + periodStr,
                "These are the current stats for **"+periodStr.toUpperCase()+"**"
        )
                        .addField("Commands Executed", String.valueOf(commandsUsed), false)
                        .setTimestamp(Instant.now())
                .build()).queue();
    }

    @Override
    public String getName() {
        return "statistics";
    }

    @Override
    public List<String> getAliases() {
        return List.of("stats");
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
