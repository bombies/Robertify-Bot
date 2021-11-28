package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

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
            
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting it to this channel.\n" +
                    "\n_You can change the announcement channel by using the \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        msg.replyEmbeds(getNowPlayingEmbed(ctx.getGuild(), selfVoiceState, memberVoiceState).build()).queue();
    }

    public EmbedBuilder getNowPlayingEmbed(Guild guild, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
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

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
        AudioPlayer audioPlayer = musicManager.audioPlayer;
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            return eb;
        }

        AudioTrackInfo info = track.getInfo();



        double progress = (double)audioPlayer.getPlayingTrack().getPosition() / track.getDuration();
        final User requester = PlayerManager.getRequester(track);
        eb =  EmbedUtils.embedMessage("🔊  `"+info.title+ " - "+info.author+"`" + (
                ((new TogglesConfig().getToggle(guild, Toggles.SHOW_REQUESTER))) && requester != null ?
                        " [ Requested by " + requester.getAsMention() + " ]"
                        :
                        ""
        ) +
                "\n\n`[0:00]`" +
                GeneralUtils.progressBar(progress, GeneralUtils.ProgressBar.DURATION) + "`["+ GeneralUtils.formatTime(track.getDuration()) +"]`\n\n" +
                "⌚  **Time left**: `"+ GeneralUtils.formatTime(track.getDuration()-audioPlayer.getPlayingTrack().getPosition())+"`\n" +
                "\n🔇 " + GeneralUtils.progressBar((double)(audioPlayer.getVolume())/100, GeneralUtils.ProgressBar.FILL) + " 🔊");

        eb.setAuthor("Now Playing", info.uri, BotConstants.ICON_URL.toString());

        return eb;
    }

    @Override
    public String getName() {
        return "nowplaying";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Displays the song that is currently playing";
    }

    @Override
    public List<String> getAliases() {
        return List.of("np");
    }
}
