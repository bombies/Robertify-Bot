package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.database.BotDB;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
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
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        BotDB botUtils = new BotDB();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting it to this channel.\n" +
                    "\n_You can change the announcement channel by using the \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        msg.replyEmbeds(handleSkip(selfVoiceState, memberVoiceState).build()).queue();
    }

    public EmbedBuilder handleSkip(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
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

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        AudioPlayer audioPlayer = musicManager.audioPlayer;

        if (audioPlayer.getPlayingTrack() == null) {
            eb = EmbedUtils.embedMessage("There is nothing to skip!");
            return eb;
        }

        audioPlayer.getPlayingTrack().setPosition(0);
        musicManager.scheduler.getPastQueue().push(audioPlayer.getPlayingTrack().makeClone());
        musicManager.scheduler.nextTrack();

        if (new DedicatedChannelConfig().isChannelSet(musicManager.scheduler.getGuild().getId()))
            new DedicatedChannelConfig().updateMessage(musicManager.scheduler.getGuild());

        return EmbedUtils.embedMessage("Skipped the song!");
    }

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Skips a track";
    }

    @Override
    public List<String> getAliases() {
        return List.of("next");
    }
}
