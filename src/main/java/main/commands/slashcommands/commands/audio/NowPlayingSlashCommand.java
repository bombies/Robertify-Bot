package main.commands.slashcommands.commands.audio;

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.utils.RobertifyEmbedUtils;
import main.utils.apis.robertify.imagebuilders.NowPlayingImageBuilder;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.themes.ThemesConfig;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.net.SocketTimeoutException;

public class NowPlayingSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("nowplaying")
                        .setDescription("See the song that is currently being played")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Displays the song that is currently playing";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final var guild = event.getGuild();
        final var memberVoiceState = event.getMember().getVoiceState();
        final var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var track = audioPlayer.getPlayingTrack();

        EmbedBuilder eb = null;

        if (!selfVoiceState.inAudioChannel())
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);

        else if (!memberVoiceState.inAudioChannel())
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);

        else if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        else if (track == null)
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);


        boolean ephemeralState = RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel());
        if (eb != null) {
            event.getHook()
                    .sendMessageEmbeds(eb.build())
                    .setEphemeral(ephemeralState)
                    .queue();
        } else {
            final var info = track.getInfo();
            try {
                final var image = new NowPlayingImageBuilder()
                        .setTitle(info.title)
                        .setArtistName(info.author)
                        .setAlbumImage(
                                track instanceof MirroringAudioTrack mirroringAudioTrack ?
                                        mirroringAudioTrack.getArtworkURL() :
                                        new ThemesConfig(guild).getTheme().getNowPlayingBanner()
                        )
                        .setDuration(info.length)
                        .setCurrentTime(audioPlayer.getTrackPosition())
                        .build();
                event.getHook()
                        .sendFiles(FileUpload.fromData(image))
                        .setEphemeral(ephemeralState)
                        .queue(done -> image.delete());
            } catch (SocketTimeoutException e) {
                Pages.paginateQueue(10, event);
            }

        }

    }
}
