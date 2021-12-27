package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.BotDB;
import main.utils.database.sqlite3.ServerDB;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JumpCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        msg.replyEmbeds(doJump(selfVoiceState, memberVoiceState, ctx, null).build()).queue();
    }

    public EmbedBuilder doJump(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, @Nullable CommandContext ctx, @Nullable String input) {

        EmbedBuilder eb;
        if (!selfVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            return eb;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            return eb;
        }

        GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        AudioPlayer audioPlayer = musicManager.audioPlayer;
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            return eb;
        }

        if (ctx != null)
            if (ctx.getArgs().isEmpty()) {
                eb = EmbedUtils.embedMessage("You must provide the amount of seconds to jump in the song!");
                return eb;
            } else
                return doActualJump(ctx.getArgs().get(0), track);
        else {
            if (input == null)
                throw new NullPointerException("Input string cannot be null");
            return doActualJump(input, track);
        }
    }

    private EmbedBuilder doActualJump(String input, AudioTrack track) {
        long time;
        EmbedBuilder eb;
        if (GeneralUtils.stringIsInt(input))
            time = Long.parseLong(input);
        else {
            eb = EmbedUtils.embedMessage("You must provide a valid duration to rewind");
            return eb;
        }

        if (time <= 0) {
            eb = EmbedUtils.embedMessage("The duration cannot be negative or zero!");
            return eb;
        }

        time = TimeUnit.SECONDS.toMillis(time);

        if (time > track.getDuration() - time) {
            eb = EmbedUtils.embedMessage("This duration cannot be more than the time left!");
            return eb;
        }

        track.setPosition(track.getPosition() + time);

        return EmbedUtils.embedMessage("Successfully jumped `"+input+"` seconds ahead!");
    }

    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Skips the song by the given number of seconds\n" +
                "\nUsage: `"+ prefix+"jump <seconds_to_jump>` *(Skips the song to a specific duration)*\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("j", "ff", "fastforward");
    }
}
