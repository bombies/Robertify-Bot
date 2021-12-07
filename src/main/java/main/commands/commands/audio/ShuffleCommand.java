package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShuffleCommand implements ICommand {
    //TODO Add shuffle slash command

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();

        msg.replyEmbeds(handleShuffle(ctx.getGuild()).build()).queue();
    }

    public EmbedBuilder handleShuffle(Guild guild) {
        final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;


        if (queue.isEmpty())
            return EmbedUtils.embedMessage("There is nothing in the queue.");
        

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

        if (new DedicatedChannelConfig().isChannelSet(guild.getId()))
            new DedicatedChannelConfig().updateMessage(guild);

        return EmbedUtils.embedMessage("Shuffled the queue!");
    }

    @Override
    public String getName() {
        return "shuffle";
    }

    @Override
    public String getHelp(String guildID) {
        return "Shuffle the current queue";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rubble");
    }
}
