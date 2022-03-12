package main.commands.prefixcommands.audio;

import lavalink.client.player.IPlayer;
import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Deprecated @ForRemoval
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

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(selfVoiceState.getGuild());
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
                return doActualJump(ctx.getGuild(), memberVoiceState.getMember().getUser(), ctx.getArgs().get(0), audioPlayer, track);
        else {
            if (input == null)
                throw new NullPointerException("Input string cannot be null");
            return doActualJump(ctx.getGuild(), memberVoiceState.getMember().getUser(), input, audioPlayer, track);
        }
    }

    private EmbedBuilder doActualJump(Guild guild, User jumper, String input, IPlayer player, AudioTrack track) {
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

        if (time > track.getInfo().getLength() - player.getTrackPosition()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "This duration cannot be more than the time left!");
            return eb;
        }

        player.seekTo(player.getTrackPosition() + time);
        new LogUtils().sendLog(guild, LogType.TRACK_JUMP, jumper.getAsMention() + " has jumped `"+TimeUnit.MILLISECONDS.toSeconds(time)+"` seconds.");

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
