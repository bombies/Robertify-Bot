package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
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
public class MoveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final var queue = musicManager.getScheduler().queue;
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final var guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed(ctx.getGuild(), "Queue");

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a song to move in the queue and the position to move it to.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        } else if (args.size() < 2) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the position to move it the song to.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(0))) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid integer as the ID.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        } else if (!GeneralUtils.stringIsInt(args.get(1))) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid integer as the position.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final int id = Integer.parseInt(args.get(0));
        final int position = Integer.parseInt(args.get(1));

        msg.replyEmbeds(handleMove(ctx.getGuild(), ctx.getAuthor(), queue, id, position).build()).queue();

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    public EmbedBuilder handleMove(Guild guild, User mover, ConcurrentLinkedQueue<AudioTrack> queue, int id, int position) {
        GeneralUtils.setCustomEmbed(guild, "Queue");

        if (queue.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_IN_QUEUE);

        final List<AudioTrack> trackList = new ArrayList<>(queue);

        if (id <= 0 || id > trackList.size()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.MoveMessages.INVALID_SONG_ID);
            return eb;
        } else if (position <= 0 || position > trackList.size()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.MoveMessages.INVALID_POSITION_ID);
            return eb;
        }

        final List<AudioTrack> prevList = new ArrayList<>(queue);
        queue.clear();
        prevList.remove(trackList.get(id-1));
        prevList.add(position-1, trackList.get(id-1));
        if (!queue.addAll(prevList)) {
            queue.addAll(trackList);
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.MoveMessages.COULDNT_MOVE, Pair.of("{id}", String.valueOf(id)));
        }

        if (new DedicatedChannelConfig(guild).isChannelSet())
            new DedicatedChannelConfig(guild).updateMessage();

        AudioTrackInfo info = trackList.get(id - 1).getInfo();

        new LogUtils(guild).sendLog(LogType.TRACK_MOVE,
                RobertifyLocaleMessage.MoveMessages.MOVED_LOG,
                Pair.of("{user}", mover.getAsMention()),
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author),
                Pair.of("{position}", String.valueOf(position))
        );
        return RobertifyEmbedUtils.embedMessage(guild,
                RobertifyLocaleMessage.MoveMessages.MOVED,
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author),
                Pair.of("{position}", String.valueOf(position))
        );
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "\nMove a specific track to a specific position in the queue\n" +
                "\nUsage: `"+ prefix+"move <id> <position>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("m");
    }
}
