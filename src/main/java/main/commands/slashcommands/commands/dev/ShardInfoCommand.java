package main.commands.slashcommands.commands.dev;

import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.ShardCacheView;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.duncte123.botcommons.StringUtils.capitalizeFully;

public class ShardInfoCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("shardinfo")
                        .setDescription("Get the information about all shards related to the bot!")
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

        final EmbedBuilder embedBuilder = RobertifyEmbedUtils.getEmbedBuilder(event.getGuild());
        final int currentShard = event.getJDA().getShardInfo().getShardId();
        final ShardManager shardManager = event.getJDA().getShardManager();
        final ShardCacheView shardCache = shardManager.getShardCache();
        final List<JDA> shards = new ArrayList<>(shardCache.asList());
        Collections.reverse(shards);

        for (final JDA shard : shards) {
            final StringBuilder valueBuilder = new StringBuilder();

            valueBuilder.append("**Status:** ").append(getShardStatus(shard))
                    .append("\n**Ping:** ").append(shard.getGatewayPing())
                    .append("\n**Guilds:** ").append(shard.getGuildCache().size());

            final int shardId = shard.getShardInfo().getShardId();

            embedBuilder.addField(String.format("Shard #%s%s", shardId, shardId == currentShard ? " (current)" : ""),
                    valueBuilder.toString(), true);
        }

        final long connectedShards = shardCache.applyStream((s) -> s.filter((shard) -> shard.getStatus() == JDA.Status.CONNECTED).count());
        final String avgPing = new DecimalFormat("###").format(shardManager.getAverageGatewayPing());
        final long guilds = shardManager.getGuildCache().size();

        embedBuilder.addField(
                "Total/Average",
                String.format("**Connected:** %s\n**Ping:** %s\n**Guilds:** %s",
                        connectedShards,
                        avgPing,
                        guilds
                ),
                false
        );

        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
    }

    private String getShardStatus(JDA shard) {
        return capitalizeFully(shard.getStatus().toString().replace('_', ' '));
    }
}
