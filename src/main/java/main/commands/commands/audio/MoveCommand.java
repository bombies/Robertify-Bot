package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MoveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            System.out.println("there is no announcement channel set");
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

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide the ID of a song to move in the queue and the position to move it to.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        } else if (args.size() < 2) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide the position to move it the song to.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(0))) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid integer as the ID.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        } else if (!GeneralUtils.stringIsInt(args.get(1))) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid integer as the position.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final int id = Integer.parseInt(args.get(0));
        final int position = Integer.parseInt(args.get(1));
        final List<AudioTrack> trackList = new ArrayList<>(queue);

        if (id <= 0 || id > trackList.size()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("That isn't a valid song id.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        } else if (position <= 0 || position > trackList.size()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("That isn't a valid position id.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }


        final List<AudioTrack> prevList = new ArrayList<>(queue);
        queue.clear();
        prevList.remove(trackList.get(id-1));
        prevList.add(position-1, trackList.get(id-1));
        if (!queue.addAll(prevList)) {
            queue.addAll(trackList);
            Listener.LOGGER.error("Could not move track with id "+id+" in the queue");
            msg.addReaction("❌").queue();
        }

        EmbedBuilder eb = EmbedUtils.embedMessage("Moved `"+trackList.get(id-1).getInfo().title
                +"` to position `"+position+"`.");
        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "\nMove a specific track to a specific position in the queue\n" +
                "\nUsage: `"+ ServerUtils.getPrefix(Long.parseLong(guildID))+"move <id> <position>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("m");
    }
}
