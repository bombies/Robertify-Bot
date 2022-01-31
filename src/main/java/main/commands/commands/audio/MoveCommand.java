package main.commands.commands.audio;

import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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

        msg.replyEmbeds(handleMove(ctx.getGuild(), queue, id, position).build()).queue();

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    public EmbedBuilder handleMove(Guild guild, ConcurrentLinkedQueue<AudioTrack> queue, int id, int position) {
        GeneralUtils.setCustomEmbed(guild, "Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing in the queue.");
            return eb;
        }

        final List<AudioTrack> trackList = new ArrayList<>(queue);

        if (id <= 0 || id > trackList.size()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "That isn't a valid song id.");
            return eb;
        } else if (position <= 0 || position > trackList.size()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "That isn't a valid position id.");
            return eb;
        }

        final List<AudioTrack> prevList = new ArrayList<>(queue);
        queue.clear();
        prevList.remove(trackList.get(id-1));
        prevList.add(position-1, trackList.get(id-1));
        if (!queue.addAll(prevList)) {
            queue.addAll(trackList);
            return RobertifyEmbedUtils.embedMessage(guild, "Could not move track with id "+id+" in the queue");
        }

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        return RobertifyEmbedUtils.embedMessage(guild, "Moved `"+trackList.get(id-1).getInfo().getTitle()
                +"` to position `"+position+"`.");
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
