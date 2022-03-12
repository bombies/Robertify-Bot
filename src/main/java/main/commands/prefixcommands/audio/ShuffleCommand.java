package main.commands.prefixcommands.audio;

import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Deprecated @ForRemoval
public class ShuffleCommand implements ICommand {

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();

        msg.replyEmbeds(handleShuffle(ctx.getGuild(), ctx.getAuthor()).build()).queue();
    }

    public EmbedBuilder handleShuffle(Guild guild, User shuffler) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var queue = musicManager.getScheduler().queue;

        if (queue.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing in the queue.");

        final List<AudioTrack> trackList = new ArrayList<>(queue);
        Collections.shuffle(trackList);

        queue.clear();
        queue.addAll(trackList);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        new LogUtils().sendLog(guild, LogType.QUEUE_SHUFFLE, shuffler.getAsMention() + " has shuffled the queue");
        return RobertifyEmbedUtils.embedMessage(guild, "Shuffled the queue!");
    }

    @Override
    public String getName() {
        return "shuffle";
    }

    @Override
    public String getHelp(String prefix) {
        return "Shuffle the current queue";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rubble");
    }
}
