package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.database.BotDB;
import main.utils.database.ServerDB;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();

        BotDB botUtils = new BotDB();
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

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide the ID of a song to remove from the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(0))) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a valid integer as the ID.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final int id = Integer.parseInt(args.get(0));
        msg.replyEmbeds(handleRemove(queue, id).build()).queue();
    }

    public EmbedBuilder handleRemove(ConcurrentLinkedQueue<AudioTrack> queue, int id) {
        GeneralUtils.setCustomEmbed("Queue");
        final List<AudioTrack> trackList = new ArrayList<>(queue);

        if (queue.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("There is nothing in the queue.");
            return eb;
        }

        EmbedBuilder eb = EmbedUtils.embedMessage("Removing `"+trackList.get(id-1).getInfo().title
                +"` from the queue");

        if (!queue.remove(trackList.get(id-1))) {
            eb =  EmbedUtils.embedMessage("Could not remove track with id "+id+" from the queue");
        }

        return eb;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Remove a specific song from the queue\n" +
                "\nUsage: `"+ ServerDB.getPrefix(Long.parseLong(guildID))+"remove <id>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("robbery");
    }
}
