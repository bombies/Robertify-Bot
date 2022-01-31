package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
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
        final var guild = ctx.getGuild();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the position you would like to jump to").build())
                    .queue();
            return;
        }

        var position = ctx.getArgs().get(0);

        if (!Pattern.matches("^\\d{2}:\\d{2}$", position)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the position in the format `mm:ss`").build())
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
        final var guild = selfVoiceState.getGuild();

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "I must be in a voice channel for this command to work");

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel I am in order to use this command");

        if (memberVoiceState.getChannel().getIdLong() != selfVoiceState.getChannel().getIdLong())
            return RobertifyEmbedUtils.embedMessage(guild, "You must bein the same voice channel I am in order to use this command");

        final var musicManager = RobertifyAudioManager.getInstance().getLavaLinkMusicManager(selfVoiceState.getGuild());
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");

        if (mins < 0 || mins > 59)
            return RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid amount of minutes.");


        if (sec < 0 || sec > 59)
            return RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid amount of seconds.");


        long totalDurationInMillis = TimeUnit.MINUTES.toMillis(mins) + TimeUnit.SECONDS.toMillis(sec);

        if (totalDurationInMillis > audioPlayer.getPlayingTrack().getDuration())
            return RobertifyEmbedUtils.embedMessage(guild, "The position provided is greater than the length of the playing track");

        audioPlayer.getPlayingTrack().setPosition(totalDurationInMillis);
        return RobertifyEmbedUtils.embedMessage(guild, "You have seeked `"+ (mins > 9 ? mins : "0" + mins) +
                ":"+ (sec > 9 ? sec : "0" + sec) +"`!");

    }

    @Override
    public String getName() {
        return "seek";
    }

    @Override
    public String getHelp(String prefix) {
        return "Jump to a specific position in the current song\n" +
                "\nUsage: `"+ prefix+"seek <mm:ss>`\n";
    }
}
