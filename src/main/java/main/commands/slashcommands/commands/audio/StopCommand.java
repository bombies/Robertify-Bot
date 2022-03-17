package main.commands.slashcommands.commands.audio;

import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.GuildMusicManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.commands.slashcommands.commands.audio.LofiCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class StopCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Guild guild = ctx.getGuild();
        final EmbedBuilder eb;
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());

        msg.replyEmbeds(handleStop(ctx.getMember()).build()).queue();
    }

    public EmbedBuilder handleStop(Member stopper) {
        Guild guild = stopper.getGuild();
        final var selfVoiceState = guild.getSelfMember().getVoiceState();
        final var memberVoiceState = stopper.getVoiceState();

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");


        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());;

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();
        final AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null)
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");

        scheduler.repeating = false;
        scheduler.playlistRepeating = false;
        audioPlayer.stopTrack();
        scheduler.queue.clear();
        scheduler.getPastQueue().clear();

        if (audioPlayer.isPaused())
            audioPlayer.setPaused(false);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        LofiCommand.getLofiEnabledGuilds().remove(guild.getIdLong());

        new LogUtils().sendLog(guild, LogType.PLAYER_STOP, stopper.getAsMention() + " has stopped the player");

        scheduler.scheduleDisconnect(true);
        return RobertifyEmbedUtils.embedMessage(guild, "You have stopped the track and cleared the queue.");
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getHelp(String prefix) {
        return "Forces the bot to stop playing music and clear the queue" +
                " if already playing music.";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("stop")
                        .setDescription("Stop the music and clear the queue")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Forces the bot to stop playing music and clear the queue" +
                " if already playing music.";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        event.replyEmbeds(handleStop(event.getMember())
                .build())
                .queue();
    }
}
