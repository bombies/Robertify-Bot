package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.lavalink.LavaLinkGuildMusicManager;
import main.audiohandlers.lavaplayer.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
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
        final var guild = memberVoiceState.getGuild();

        EmbedBuilder eb;
        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            return eb;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            return eb;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getLavaLinkMusicManager(selfVoiceState.getGuild());
        final var audioPlayer = musicManager.getPlayer();
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            return eb;
        }

        if (ctx != null)
            if (ctx.getArgs().isEmpty()) {
                eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide the amount of seconds to jump in the song!");
                return eb;
            } else
                return doActualJump(ctx.getGuild(), ctx.getArgs().get(0), track);
        else {
            if (input == null)
                throw new NullPointerException("Input string cannot be null");
            return doActualJump(ctx.getGuild(), input, track);
        }
    }

    private EmbedBuilder doActualJump(Guild guild, String input, AudioTrack track) {
        long time;
        EmbedBuilder eb;
        if (GeneralUtils.stringIsInt(input))
            time = Long.parseLong(input);
        else {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid duration to rewind");
            return eb;
        }

        if (time <= 0) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "The duration cannot be negative or zero!");
            return eb;
        }

        time = TimeUnit.SECONDS.toMillis(time);

        if (time > track.getDuration() - track.getPosition()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "This duration cannot be more than the time left!");
            return eb;
        }

        track.setPosition(track.getPosition() + time);

        return RobertifyEmbedUtils.embedMessage(guild, "Successfully jumped `"+input+"` seconds ahead!");
    }

    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Skips the song by the given number of seconds\n" +
                "\nUsage: `"+ prefix+"jump <seconds_to_jump>` *(Skips the song to a specific duration)*\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("j", "ff", "fastforward");
    }
}
