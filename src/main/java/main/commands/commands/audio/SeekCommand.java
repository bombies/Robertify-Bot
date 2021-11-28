package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.database.ServerDB;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class SeekCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide the position you would like to jump to").build())
                    .queue();
            return;
        }

        var position = ctx.getArgs().get(0);

        if (!Pattern.matches("^\\d{2}:\\d{2}$", position)) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide the position in the format `mm:ss`").build())
                    .queue();
            return;
        }

        String[] positionSplit = position.split(":");
        int mins = Integer.parseInt(positionSplit[0]);
        int secs = Integer.parseInt(positionSplit[1]);

        final GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();

        msg.replyEmbeds(handleSeek(selfVoiceState, memberVoiceState, mins, secs).build())
                .queue();
    }

    public EmbedBuilder handleSeek(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, int mins, int sec) {
        if (!selfVoiceState.inVoiceChannel())
            return EmbedUtils.embedMessage("I must be in a voice channel for this command to work");

        if (!memberVoiceState.inVoiceChannel())
            return EmbedUtils.embedMessage("You must be in the same voice channel I am in order to use this command");

        if (memberVoiceState.getChannel().getIdLong() != selfVoiceState.getChannel().getIdLong())
            return EmbedUtils.embedMessage("You must bein the same voice channel I am in order to use this command");

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;

        if (audioPlayer.getPlayingTrack() == null)
            return EmbedUtils.embedMessage("There is nothing playing!");

        if (mins < 0 || mins > 59)
            return EmbedUtils.embedMessage("You must provide a valid amount of minutes.");


        if (sec < 0 || sec > 59)
            return EmbedUtils.embedMessage("You must provide a valid amount of seconds.");


        long totalDurationInMillis = TimeUnit.MINUTES.toMillis(mins) + TimeUnit.SECONDS.toMillis(sec);

        if (totalDurationInMillis > audioPlayer.getPlayingTrack().getDuration())
            return EmbedUtils.embedMessage("The position provided is greater than the length of the playing track");

        audioPlayer.getPlayingTrack().setPosition(totalDurationInMillis);
        return EmbedUtils.embedMessage("You have seeked `"+ (mins > 9 ? mins : "0" + mins) +
                ":"+ (sec > 9 ? sec : "0" + sec) +"`!");

    }

    @Override
    public String getName() {
        return "seek";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Jump to a specific position in the current song\n" +
                "\nUsage: `"+ ServerDB.getPrefix(Long.parseLong(guildID))+"seek <mm:ss>`\n";
    }
}
