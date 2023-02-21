package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.GuildMusicManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.exceptions.AutoPlayException;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

@Deprecated @ForRemoval
public class SkipToCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.getScheduler().queue;
        final Message msg = ctx.getMessage();
        final List<String> args = ctx.getArgs();
        final var guild = ctx.getGuild();

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the ID of a song to skip to.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(0))) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "ID provided **must** be a valid integer!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        int id = Integer.parseInt(args.get(0));

        msg.replyEmbeds(handleSkip(ctx.getAuthor(), queue, musicManager, id).build()).queue();
    }

    public EmbedBuilder handleSkip(User skipper, ConcurrentLinkedQueue<AudioTrack> queue, GuildMusicManager musicManager, int id) {
        if (id > queue.size() || id <= 0)
            return RobertifyEmbedUtils.embedMessage(musicManager.getGuild(), "ID provided isn't a valid ID!");

        final var audioPlayer = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();
        final var guild = musicManager.getGuild();
        List<AudioTrack> currentQueue = new ArrayList<>(queue);
        List<AudioTrack> songsToRemoveFromQueue = new ArrayList<>();

        for (int i = 0; i < id-1; i++)
            songsToRemoveFromQueue.add(currentQueue.get(i));

        queue.removeAll(songsToRemoveFromQueue);
        audioPlayer.seekTo(0);
        HashMap<Long, Stack<AudioTrack>> pastQueue = scheduler.getPastQueue();
        if (!pastQueue.containsKey(guild.getIdLong()))
            pastQueue.put(guild.getIdLong(), new Stack<>());

        AudioTrack playingTrack = audioPlayer.getPlayingTrack();
        pastQueue.get(guild.getIdLong()).push(playingTrack);

        try {
            scheduler.nextTrack(playingTrack, true, playingTrack.getPosition());
        } catch (AutoPlayException ignored) {}

        if (new RequestChannelConfig(guild).isChannelSet())
            new RequestChannelConfig(guild).updateMessage();

        new LogUtils(guild).sendLog(LogType.TRACK_SKIP, skipper.getAsMention() + " has skipped to `track #"+id+"`");
        SkipCommand.clearVoteSkipInfo(guild);
        return RobertifyEmbedUtils.embedMessage(musicManager.getGuild(), "Skipped to **track #"+id+"**!");
    }

    @Override
    public String getName() {
        return "skipto";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Skip to a specific song in the queue\n" +
                "\nUsage: `"+ prefix+"skipto <id>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("st");
    }
}
