package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class PreviousTrackCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();
        final Member member = ctx.getMember();

        msg.replyEmbeds(handlePrevious(guild, member.getVoiceState()).build())
                .queue();
    }

    public EmbedBuilder handlePrevious(Guild guild, GuildVoiceState memberVoiceState) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var scheduler = musicManager.getScheduler();
        final var queueHandler = scheduler.getQueueHandler();
        final var audioPlayer = musicManager.getPlayer();
        final var selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!selfVoiceState.inAudioChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED);

        if (!memberVoiceState.inAudioChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        if (queueHandler.previousTracksSize() == 0)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PreviousTrackMessages.NO_PREV_TRACKS);

        if (audioPlayer.getPlayingTrack() != null) {
            final var nowPlayingTrack = audioPlayer.getPlayingTrack();
            queueHandler.addToBeginning(nowPlayingTrack);
            audioPlayer.stopTrack();
        }

        audioPlayer.playTrack(queueHandler.popPreviousTrack());

        if (new RequestChannelConfig(guild).isChannelSet())
            new RequestChannelConfig(guild).updateMessage();

        new LogUtils(guild).sendLog(LogType.TRACK_PREVIOUS, RobertifyLocaleMessage.PreviousTrackMessages.PREV_TRACK_LOG, Pair.of("{user}", memberVoiceState.getMember().getAsMention()));
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PreviousTrackMessages.PLAYING_PREV_TRACK);
    }

    @Override
    public String getName() {
        return "previous";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Go back to he track that was played previously\n\n" +
                "**Usage**: `"+ prefix +"previous`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("prev");
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("previous")
                        .setDescription("Go to the previous track!")
                        .setPremium()
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return """
                Go back to he track that was played previously

                **Usage**: `/previous`""";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checksWithPremium(event)) return;
        sendRandomMessage(event);

        event.replyEmbeds(handlePrevious(event.getGuild(), event.getMember().getVoiceState()).build())
                .queue();
    }
}
