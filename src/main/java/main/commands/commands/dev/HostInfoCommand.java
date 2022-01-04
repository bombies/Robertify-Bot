package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;

import javax.script.ScriptException;
import java.util.List;

public class HostInfoCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        String os = System.getProperty("os.name");
        String osVer = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String javaVer = System.getProperty("java.version");
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long freeMemory = Runtime.getRuntime().freeMemory() / 1048576;
        long maxMemory = Runtime.getRuntime().maxMemory() / 1048576;
        long memoryInUse = Runtime.getRuntime().totalMemory() / 1048576;

        ctx.getMessage().replyEmbeds(EmbedUtils.embedMessageWithTitle(
                "Host Information",
                "\t"
                )
                        .addField("OS Name", os, true)
                        .addField("OS Version", osVer, true)
                        .addField("OS Architecture", osArch, true)
                        .addBlankField(false)
                        .addField("Available Processors", String.valueOf(availableProcessors), true)
                        .addField("Java Version", javaVer, true)
                        .addBlankField(false)
                        .addField("Free Memory (MB)", String.valueOf(freeMemory), true)
                        .addField("Memory In Use (MB)", String.valueOf(memoryInUse), true)
                        .addField("Max Memory (MB)", String.valueOf(maxMemory), true)
                        .build())
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
