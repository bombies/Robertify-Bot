package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class SkipCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState).build()).queue();
    }

    public EmbedBuilder handleSkip(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();
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

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing to skip!");
            return eb;
        }

        audioPlayer.getPlayingTrack().setPosition(0);
        musicManager.getScheduler().getPastQueue().push(audioPlayer.getPlayingTrack().makeClone());
        musicManager.getScheduler().nextTrack();

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        LofiCommand.getLofiEnabledGuilds().remove(guild.getIdLong());

        return RobertifyEmbedUtils.embedMessage(guild, "Skipped the song!");
    }

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Skips a track";
    }

    @Override
    public List<String> getAliases() {
        return List.of("next");
    }
}
