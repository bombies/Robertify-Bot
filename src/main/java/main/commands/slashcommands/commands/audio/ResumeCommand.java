package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class ResumeCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final var guild = ctx.getGuild();

        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!audioPlayer.isPaused()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "The player isn't paused!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        audioPlayer.setPaused(false);
        eb = RobertifyEmbedUtils.embedMessage(guild, "You have resumed the song!");
        new LogUtils().sendLog(guild, LogType.PLAYER_RESUME, member.getAsMention() + " has resumed the music");
        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "resume";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("resume")
                        .setDescription("Resume paused tracks")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return """
                Resumes the currently playing song if paused

                Usage: `/resume`""";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Resumes the currently playing song if paused\n" +
                "\nUsage: `"+ prefix+"resume`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("res");
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final var guild = event.getGuild();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        EmbedBuilder eb;

        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing");
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!audioPlayer.isPaused()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "The player isn't paused!");
            event.replyEmbeds(eb.build()).queue();
            return;
        }

        audioPlayer.setPaused(false);
        eb = RobertifyEmbedUtils.embedMessage(guild, "You have resumed the song!");
        new LogUtils().sendLog(guild, LogType.PLAYER_RESUME, member.getAsMention() + " has resumed the music");
        event.replyEmbeds(eb.build()).queue();
    }
}
