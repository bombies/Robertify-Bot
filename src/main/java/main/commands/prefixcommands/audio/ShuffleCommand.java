package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.internal.utils.tuple.Pair;

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
        final var queueHandler = musicManager.getScheduler().getQueueHandler();

        if (queueHandler.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_IN_QUEUE);

        final List<AudioTrack> trackList = new ArrayList<>(queueHandler.contents());
        Collections.shuffle(trackList);

        queueHandler.clear();
        queueHandler.addAll(trackList);

        if (new RequestChannelConfig(guild).isChannelSet())
            new RequestChannelConfig(guild).updateMessage();

        new LogUtils(guild).sendLog(LogType.QUEUE_SHUFFLE, RobertifyLocaleMessage.ShuffleMessages.SHUFFLED_LOG, Pair.of("{user}", shuffler.getAsMention()));
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShuffleMessages.SHUFFLED);
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
