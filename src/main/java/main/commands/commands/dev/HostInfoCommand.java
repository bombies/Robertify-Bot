package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.utils.RobertifyEmbedUtils;

import javax.script.ScriptException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

public class HostInfoCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var guild = ctx.getGuild();

        Runtime runtime = Runtime.getRuntime();
        NumberFormat format = NumberFormat.getInstance();
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        final long maxMemory = runtime.maxMemory() / 1048576;
        final long memoryInUse = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
        final long freeMemory = (runtime.maxMemory() / 1048576) - memoryInUse;

        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(
                guild,
                "Host Information",
                "\t"
                )
                        .addField("Usage", "```java\n" +
                                "Total - "+format.format(maxMemory)+" MB\n" +
                                "Used - "+format.format(memoryInUse)+" MB - "+decimalFormat.format(((double)memoryInUse/maxMemory)*100)+"%\n" +
                                "Free - "+format.format(freeMemory)+"MB - "+decimalFormat.format(((double)freeMemory/maxMemory)*100)+"%```", false).build())
                .queue();
    }

    @Override
    public String getName() {
        return "hostinfo";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("hi");
    }
}
