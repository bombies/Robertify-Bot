package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
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

public class SkipToCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            EmbedBuilder eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting it to this channel.\n" +
                    "\n_You can change the announcement channel by using the \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide the ID of a song to skip to.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(0))) {
            EmbedBuilder eb = EmbedUtils.embedMessage("ID provided **must** be a valid integer!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int id = Integer.parseInt(args.get(0));

        msg.replyEmbeds(handleSkip(queue, musicManager, id).build()).queue();
    }

    public EmbedBuilder handleSkip(ConcurrentLinkedQueue<AudioTrack> queue, GuildMusicManager musicManager, int id) {
        if (id > queue.size() || id <= 0)
            return EmbedUtils.embedMessage("ID provided isn't a valid ID!");

        final var audioPlayer = musicManager.audioPlayer;
        List<AudioTrack> currentQueue = new ArrayList<>(queue);
        List<AudioTrack> songsToRemoveFromQueue = new ArrayList<>();

        for (int i = 0; i < id-1; i++)
            songsToRemoveFromQueue.add(currentQueue.get(i));

        queue.removeAll(songsToRemoveFromQueue);
        audioPlayer.getPlayingTrack().setPosition(0);
        musicManager.scheduler.getPastQueue().push(audioPlayer.getPlayingTrack().makeClone());
        musicManager.scheduler.nextTrack();

        return EmbedUtils.embedMessage("Skipped to **track #"+id+"**!");
    }

    @Override
    public String getName() {
        return "skipto";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Skip to a specific song in the queue\n" +
                "\nUsage: `"+ ServerUtils.getPrefix(Long.parseLong(guildID))+"skipto <id>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("st");
    }
}
