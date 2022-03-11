package main.commands.commands.audio;

import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Deprecated @ForRemoval
public class RemoveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.getScheduler().queue;
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final var guild = ctx.getGuild();

        GeneralUtils.setCustomEmbed(ctx.getGuild(),  "Queue");

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
        GeneralUtils.setCustomEmbed(guild, "Queue");
        final List<AudioTrack> trackList = new ArrayList<>(queue);

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing in the queue.");
            return eb;
        }

        if (id <= 0 || id > queue.size())
            return RobertifyEmbedUtils.embedMessage(guild, "This is an invalid ID! You must provide an ID between 1 and " + queue.size());

        AudioTrack removedTrack = trackList.get(id - 1);
        AudioTrackInfo info = removedTrack.getInfo();
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Removing `"+ info.getTitle()
                +"` from the queue");

        if (!queue.remove(removedTrack))
            eb =  RobertifyEmbedUtils.embedMessage(guild, "Could not remove track with id "+id+" from the queue");
        else
            new LogUtils().sendLog(guild, LogType.QUEUE_REMOVE, remover.getAsMention() + " has removed `"+ info.getTitle() +" by "+ info.getAuthor() +"` from the queue.");

        if (id <= 10)
            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
                new DedicatedChannelConfig().updateMessage(guild);

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
