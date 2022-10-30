package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.pagination.Pages;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
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

        sendQueue(ctx.getGuild(), queue, ctx.getChannel(), ctx.getAuthor());
        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    private void sendQueue(Guild guild, ConcurrentLinkedQueue<AudioTrack> queue, TextChannel channel, User user) {
        final List<AudioTrack> trackList = new ArrayList<>(queue);

        List<String> content = getContent(guild, queue, trackList);

        Pages.paginateMessage(channel, user, content, 10);
    }

    public List<String> getContent(Guild guild, ConcurrentLinkedQueue<AudioTrack> queue, List<AudioTrack> trackList) {
        List<String> content = new ArrayList<>();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        for (int i = 0; i < queue.size(); i++) {
            final AudioTrack track = trackList.get(i);
            final AudioTrackInfo info = track.getInfo();
            content.add(localeManager.getMessage(RobertifyLocaleMessage.QueueMessages.QUEUE_ENTRY,
                    Pair.of("{id}", String.valueOf(i+1)),
                    Pair.of("{title}", info.title),
                    Pair.of("{author}", info.author),
                    Pair.of("{duration}", GeneralUtils.formatTime(track.getInfo().length))
            ));
        }
        return content;
    }

    public List<String> getContent(Guild guild, Stack<AudioTrack> pastTracks) {
        List<String> content = new ArrayList<>();
        final var trackList = new ArrayList<>(pastTracks.stream().toList());
        Collections.reverse(trackList);
        final var localeManager = LocaleManager.getLocaleManager(guild);
        for (int i = 0; i < pastTracks.size(); i++) {
            final AudioTrack track = trackList.get(i);
            final AudioTrackInfo info = track.getInfo();
            content.add(localeManager.getMessage(RobertifyLocaleMessage.QueueMessages.QUEUE_ENTRY,
                    Pair.of("{id}", String.valueOf(i+1)),
                    Pair.of("{title}", info.title),
                    Pair.of("{author}", info.author),
                    Pair.of("{duration}", GeneralUtils.formatTime(track.getInfo().length))
            ));
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
