package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.pagination.Pages;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Deprecated @ForRemoval
public class QueueCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final var queue = musicManager.getScheduler().queue;
        final Message msg = ctx.getMessage();

        GeneralUtils.setCustomEmbed(ctx.getGuild(), "Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "There is nothing in the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        sendQueue(queue, ctx.getChannel(), ctx.getAuthor());
        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    private void sendQueue(ConcurrentLinkedQueue<AudioTrack> queue, TextChannel channel, User user) {
        final List<AudioTrack> trackList = new ArrayList<>(queue);

        List<String> content = getContent(queue, trackList);

        Pages.paginateMessage(channel, user, content, 10);
    }

    public List<String> getContent(ConcurrentLinkedQueue<AudioTrack> queue, List<AudioTrack> trackList) {
        List<String> content = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            final AudioTrack track = trackList.get(i);
            final AudioTrackInfo info = track.getInfo();
            content.add("**#"+(i+1)+".** "+info.title+" - "+info.author+" `["+ GeneralUtils.formatTime(track.getInfo().length)+"]`");
        }
        return content;
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Shows all the queued songs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("q");
    }
}
