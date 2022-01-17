package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.sources.RobertifyAudioTrack;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class NowPlayingCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(NowPlayingCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        msg.replyEmbeds(getNowPlayingEmbed(ctx.getGuild(), selfVoiceState, memberVoiceState).build()).queue();
    }

    public EmbedBuilder getNowPlayingEmbed(Guild guild, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
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
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command");
            return eb;
        }

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        var audioPlayer = musicManager.getPlayer();
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            return eb;
        }

        AudioTrackInfo info = track.getInfo();

        double progress = (double)audioPlayer.getPlayingTrack().getPosition() / track.getDuration();
        final User requester = RobertifyAudioManager.getRequester(track);
        eb =  RobertifyEmbedUtils.embedMessageWithTitle(guild, (LofiCommand.getLofiEnabledGuilds().contains(guild.getIdLong())
                ? "Lo-Fi Music"
                        :
                info.title + " by "  + info.author),

                (((new TogglesConfig().getToggle(guild, Toggles.SHOW_REQUESTER))) && requester != null ?
                        "\n\n~ Requested by " + requester.getAsMention()
                        :
                        "") +
                "\n\n "+ (info.isStream ? "" : "`[0:00]`") +
                        (LofiCommand.getLofiEnabledGuilds().contains(guild.getIdLong()) ? "" : (
                GeneralUtils.progressBar(progress, GeneralUtils.ProgressBar.DURATION) + (info.isStream ? "" : "`["+ GeneralUtils.formatTime(track.getDuration()) +"]`") + "\n\n" +
                (info.isStream ?
                        "📺 **[Livestream]**\n"
                                :
                        "⌚  **Time left**: `"+ GeneralUtils.formatTime(track.getDuration()-audioPlayer.getPlayingTrack().getPosition()) + "`\n") +

                "\n🔇 " + GeneralUtils.progressBar((double)(audioPlayer.getVolume())/100, GeneralUtils.ProgressBar.FILL) + " 🔊")));

        if (track instanceof RobertifyAudioTrack robertifyAudioTrack)
            eb.setThumbnail(robertifyAudioTrack.getTrackImage());

        eb.setAuthor("Now Playing", GeneralUtils.isUrl(info.uri) ? info.uri : null, new ThemesConfig().getTheme(guild.getIdLong()).getTransparent());

        return eb;
    }

    @Override
    public String getName() {
        return "nowplaying";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Displays the song that is currently playing";
    }

    @Override
    public List<String> getAliases() {
        return List.of("np");
    }
}
