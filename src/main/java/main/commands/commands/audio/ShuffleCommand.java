package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class ShuffleCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        final BlockingQueue<AudioTrack> queue = musicManager.scheduler.queue;
        final Message msg = ctx.getMessage();

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            System.out.println("there is no announcement channel set");
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            EmbedBuilder eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting to to this channel.\n" +
                    "\n_You can change the announcement channel by using set \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        GeneralUtils.setCustomEmbed(BotConstants.ROBERTIFY_EMBED_TITLE + " | Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("There is nothing in the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final List<AudioTrack> trackList = new ArrayList<>(queue);
        List<AudioTrack> shuffledTrackList = new ArrayList<>();
        Random random = new Random();
        int trackListSize = trackList.size();

        for (AudioTrack ignored : trackList) {
            AudioTrack trackSelected = trackList.get(random.nextInt(trackListSize));
            while (shuffledTrackList.contains(trackSelected))
                trackSelected = trackList.get(random.nextInt(trackListSize));
            shuffledTrackList.add(trackSelected);
        }

        queue.clear();
        queue.addAll(shuffledTrackList);

        EmbedBuilder eb = EmbedUtils.embedMessage("Shuffled the queue!");
        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "shuffle";
    }

    @Override
    public String getHelp(String guildID) {
        return "Shuffle the current queue";
    }
}
