package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.database.BotUtils;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class RepeatCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            System.out.println("there is no announcement channel set");
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting to to this channel.\n" +
                    "\n_You can change the announcement channel by using set \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!selfVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        AudioPlayer audioPlayer = musicManager.audioPlayer;

        if (audioPlayer.getPlayingTrack() == null) {
            eb = EmbedUtils.embedMessage("There is no song playing. I can't repeat nothing.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (ctx.getArgs().isEmpty()) {
            if (musicManager.scheduler.repeating) {
                musicManager.scheduler.repeating = false;
                eb = EmbedUtils.embedMessage("`" + musicManager.audioPlayer.getPlayingTrack().getInfo().title + "` will no longer be repeated!");
            } else {
                musicManager.scheduler.repeating = true;
                eb = EmbedUtils.embedMessage("`" + musicManager.audioPlayer.getPlayingTrack().getInfo().title + "` will now be replayed");
            }
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (ctx.getArgs().get(0).equalsIgnoreCase("queue") || ctx.getArgs().get(0).equalsIgnoreCase("q")) {
            if (musicManager.scheduler.playlistRepeating) {
                musicManager.scheduler.playlistRepeating = false;
                musicManager.scheduler.removeSavedQueue(ctx.getGuild());
                eb = EmbedUtils.embedMessage("The current queue will no longer be repeated!");
            } else {
                musicManager.scheduler.playlistRepeating = true;

                AudioTrack thisTrack = audioPlayer.getPlayingTrack().makeClone();
                thisTrack.setPosition(0L);

                musicManager.scheduler.queue.offer(thisTrack);
                musicManager.scheduler.setSavedQueue(ctx.getGuild(), musicManager.scheduler.queue);
                musicManager.scheduler.queue.remove(thisTrack);
                eb = EmbedUtils.embedMessage("The current queue will now be repeated!");
            }
        } else {
            eb = EmbedUtils.embedMessage("Invalid arguments!");
        }
        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "repeat";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Set the song being replayed\n" +
                "\nUsage `" + ServerUtils.getPrefix(Long.parseLong(guildID)) + "repeat [queue]` *(Add `queue` to start repeating the current queue)*";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rep");
    }
}
