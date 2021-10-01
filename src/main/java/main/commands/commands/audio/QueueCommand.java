package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.constants.MessageButton;
import main.utils.GeneralUtils;
import main.utils.pagination.Pages;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class QueueCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        final BlockingQueue<AudioTrack> queue = musicManager.scheduler.queue;
        final Message msg = ctx.getMessage();

        GeneralUtils.setCustomEmbed(BotConstants.ROBERTIFY_EMBED_TITLE + " | Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("There is nothing in the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final List<AudioTrack> trackList = new ArrayList<>(queue);

        List<String> content = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            final AudioTrack track = trackList.get(i);
            final AudioTrackInfo info = track.getInfo();
            content.add("**#"+(i+1)+".** "+info.title+" ["+formatTime(track.getDuration())+"]");
        }

        Pages.paginate(ctx.getChannel(), ctx.getAuthor(), content, 10);

        GeneralUtils.setDefaultEmbed();
    }

    private String formatTime(long duration) {
        final long hours = duration / TimeUnit.HOURS.toMillis(1);
        final long minutes = duration / TimeUnit.MINUTES.toMillis(1);
        final long seconds = duration % TimeUnit.MINUTES.toMillis(1) / TimeUnit.SECONDS.toMillis(1);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getHelp(String guildID) {
        return "Shows all the queued songs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("q");
    }
}
