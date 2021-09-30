package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import main.audiohandlers.AudioPlayerSendHandler;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.main.Listener;
import main.main.Robertify;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.script.ScriptException;

public class PlayCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final TextChannel channel = ctx.getChannel();
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!selfVoiceState.inVoiceChannel()) {
            AudioManager audioManager = ctx.getGuild().getAudioManager();
            AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
            AudioSourceManagers.registerRemoteSources(audioPlayerManager);
            AudioPlayer player = audioPlayerManager.createPlayer();
            audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
            audioManager.openAudioConnection(memberVoiceState.getChannel());
        }

        PlayerManager.getInstance()
                .loadAndPlay(channel, "https://www.youtube.com/watch?v=7s5-73n_SjE&ab_channel=sadch%C9%A8llsadch%C9%A8ll");

    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp(String guildID) {
        return "Plays a song\n" +
                "Usage `" + ServerUtils.getPrefix(Long.parseLong(guildID)) + "play <song>`";
    }
}
