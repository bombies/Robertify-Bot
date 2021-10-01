package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.utils.database.BotUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class NowPlayingCommand implements ICommand {
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
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        AudioTrackInfo info = track.getInfo();

        eb =  EmbedUtils.embedMessage("**"+info.title+"**");
        eb.setTitle(BotConstants.ROBERTIFY_EMBED_TITLE + " | Now Playing", info.uri);

        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "nowplaying";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("np");
    }
}
