package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
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
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Deprecated @ForRemoval
public class RemoveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.getScheduler().getQueue();
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final var guild = ctx.getGuild();

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing in the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a song to remove from the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(0))) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid integer as the ID.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final int id = Integer.parseInt(args.get(0));
        msg.replyEmbeds(handleRemove(ctx.getGuild(), ctx.getAuthor(), queue, id).build()).queue();
    }

    public EmbedBuilder handleRemove(Guild guild, User remover, ConcurrentLinkedQueue<AudioTrack> queue, int id) {
        final List<AudioTrack> trackList = new ArrayList<>(queue);

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_IN_QUEUE);
            return eb;
        }

        if (id <= 0 || id > queue.size())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.RemoveMessages.REMOVE_INVALID_ID, Pair.of("{max}", String.valueOf(queue.size())));

        AudioTrack removedTrack = trackList.get(id - 1);
        AudioTrackInfo info = removedTrack.getInfo();
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.RemoveMessages.REMOVED, Pair.of("{title}", info.title), Pair.of("{author}", info.author));

        if (!queue.remove(removedTrack))
            eb =  RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.RemoveMessages.COULDNT_REMOVE, Pair.of("{id}", String.valueOf(id)));
        else
            new LogUtils(guild).sendLog(LogType.QUEUE_REMOVE, RobertifyLocaleMessage.RemoveMessages.REMOVED_LOG,
                    Pair.of("{user}", remover.getAsMention()),
                    Pair.of("{title}", info.title),
                    Pair.of("{author}", info.author)
            );

        if (id <= 10)
            if (new RequestChannelConfig(guild).isChannelSet())
                new RequestChannelConfig(guild).updateMessage();
        return eb;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Remove a specific song from the queue\n" +
                "\nUsage: `"+ prefix+"remove <id>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("robbery");
    }
}
