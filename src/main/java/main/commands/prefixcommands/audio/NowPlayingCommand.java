package main.commands.prefixcommands.audio;

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lavalink.client.io.filters.Filters;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

@Deprecated @ForRemoval
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

        msg.replyEmbeds(getNowPlayingEmbed(ctx.getGuild(), ctx.getChannel(), selfVoiceState, memberVoiceState).build()).queue();
    }

    public EmbedBuilder getNowPlayingEmbed(Guild guild, GuildMessageChannel channel, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        EmbedBuilder eb;

        if (!selfVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);
            return eb;
        }

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));
            return eb;
        }

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        var audioPlayer = musicManager.getPlayer();
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);
            return eb;
        }

        AudioTrackInfo info = track.getInfo();

        double progress = (double)audioPlayer.getTrackPosition() / track.getInfo().length;
        final Filters filters = audioPlayer.getFilters();
        final String requester = RobertifyAudioManager.getRequester(guild, track);
        final var localeManager = LocaleManager.getLocaleManager(guild);
        eb =  RobertifyEmbedUtils.embedMessageWithTitle(guild, (localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_EMBED_TITLE, Pair.of("{title}", info.title), Pair.of("{author}", info.author))),
                (((new TogglesConfig(guild).getToggle(Toggles.SHOW_REQUESTER))) && requester != null ?
                        "\n\n" + localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_REQUESTER, Pair.of("{requester}", requester))
                        :
                        "") +
                "\n\n "+ (info.isStream ? "" : "`[0:00]`") +
                        (
                GeneralUtils.progressBar(guild, channel, progress, GeneralUtils.ProgressBar.DURATION) + (info.isStream ? "" : "`["+ GeneralUtils.formatTime(track.getInfo().length) +"]`") + "\n\n" +
                (info.isStream ?
                        localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_LIVESTREAM) + "\n"
                                :
                        localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_TIME_LEFT, Pair.of("{time}", GeneralUtils.formatTime(track.getInfo().length-audioPlayer.getTrackPosition()))) + "\n") +

                "\n🔇 " + GeneralUtils.progressBar(guild, channel, filters.getVolume(), GeneralUtils.ProgressBar.FILL) + " 🔊"));

        if (track instanceof MirroringAudioTrack mirroringAudioTrack)
            eb.setImage(mirroringAudioTrack.getArtworkURL());

        eb.setAuthor(localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_AUTHOR), GeneralUtils.isUrl(info.uri) ? info.uri : null, new ThemesConfig(guild).getTheme().getTransparent());

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
