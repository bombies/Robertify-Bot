package main.commands.commands.audio;

import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import main.utils.pagination.Pages;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;
        final Message msg = ctx.getMessage();

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            EmbedBuilder eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting it to this channel.\n" +
                    "\n_You can change the announcement channel by using the \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        GeneralUtils.setCustomEmbed("Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("There is nothing in the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        sendQueue(queue, ctx.getChannel(), ctx.getAuthor());
        GeneralUtils.setDefaultEmbed();
    }

    private void sendQueue(ConcurrentLinkedQueue<AudioTrack> queue, TextChannel channel, User user) {
        final List<AudioTrack> trackList = new ArrayList<>(queue);

        List<String> content = getContent(queue, trackList);

        Pages.paginate(channel, user, content, 10);
    }

    public List<String> getContent(ConcurrentLinkedQueue<AudioTrack> queue, List<AudioTrack> trackList) {
        List<String> content = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            final AudioTrack track = trackList.get(i);
            final AudioTrackInfo info = track.getInfo();
            content.add("**#"+(i+1)+".** "+info.title+" - "+info.author+" `["+ GeneralUtils.formatTime(track.getDuration())+"]`");
        }
        return content;
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Shows all the queued songs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("q");
    }
}
