package main.commands.slashcommands.commands.dev;

import lavalink.client.io.RemoteStats;
import lavalink.client.io.jda.JdaLavalink;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.text.DecimalFormat;
import java.util.List;

public class NodeInfoCommand extends AbstractSlashCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        JdaLavalink lavalink = Robertify.getLavalink();
        DecimalFormat df = new DecimalFormat("###.##");
        StringBuilder descBuilder = new StringBuilder();
        for (final var node : lavalink.getNodes()) {
            RemoteStats stats = node.getStats();
            descBuilder.append("**__" + node.getName() + "__**\n```" +
                    "CPU Cores:" + stats.getCpuCores() + "\n" +
                    "Total Lavalink Load: " + df.format(stats.getLavalinkLoad() * 100) + "%\n" +
                    "Total System Load: " + df.format(stats.getSystemLoad() * 100) + "%\n\n" +
                    "Memory Allocated: " + df.format((stats.getMemAllocated() / 1000000)) + "MB\n" +
                    "Memory Reservable: " + df.format((stats.getMemReservable() / 1000000)) + "MB\n" +
                    "Memory Used: " + df.format((stats.getMemUsed() / 1000000)) + "MB\n" +
                    "Memory Free: " + df.format((stats.getMemFree() / 1000000)) + "MB\n\n" +
                    "Total Players: " + stats.getPlayers() + "\n" +
                    "Playing Players: " + stats.getPlayingPlayers() + "\n" +
                    "Uptime: " + GeneralUtils.getDurationString(stats.getUptime()) +
                    "```\n\n");
        }

        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), descBuilder.toString()).build())
                .queue();
    }

    @Override
    public String getName() {
        return "nodeinfo";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("ni");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("nodeinfo")
                        .setDescription("Count all the voice channels Robertify is currenly playing music in!")
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

        JdaLavalink lavalink = Robertify.getLavalink();
        DecimalFormat df = new DecimalFormat("###.##");
        StringBuilder descBuilder = new StringBuilder("```txt");
        for (final var node : lavalink.getNodes()) {
            RemoteStats stats = node.getStats();
            descBuilder.append(String.format("""
                                    ===============================
                                    `%s`
                                    CPU Cores: %d
                                    Total Lavalink Load: %s%%
                                    Total System Load: %s%%
                                    -------------------------------
                                    Memory Allocated: %sMB
                                    Memory Reservable: %sMB
                                    Memory Used: %sMB
                                    Memory Free: %sMB
                                    -------------------------------
                                    Total Players: %d
                                    Playing Players: %d
                                    Frames Sent per minute: %d
                                    Frames Nulled per minute: %d
                                    Frames Deficit per minute: %d
                                    -------------------------------
                                    Uptime: %s
                                    ===============================
                                    """,
                            node.getName(),
                            stats.getCpuCores(),
                            df.format(stats.getLavalinkLoad() * 100),
                            df.format(stats.getSystemLoad() * 100),
                            df.format((stats.getMemAllocated() / 1000000)),
                            df.format((stats.getMemReservable() / 1000000)),
                            df.format((stats.getMemUsed() / 1000000)),
                            df.format((stats.getMemFree() / 1000000)),
                            stats.getPlayers(),
                            stats.getPlayingPlayers(),
                            stats.getAvgFramesSentPerMinute(),
                            stats.getAvgFramesNulledPerMinute(),
                            stats.getAvgFramesDeficitPerMinute(),
                            GeneralUtils.getDurationString(stats.getUptime())
                    )
            );
        }

        descBuilder.append("```");

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), descBuilder.toString()).build())
                .queue();
    }
}
