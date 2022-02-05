package main.commands.commands.audio.filters;

import lavalink.client.io.filters.Rotation;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.entities.GuildVoiceState;

import javax.script.ScriptException;

public class EightDFilter implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        // TODO Paywall

        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var filters = audioPlayer.getFilters();
        final var selfMember = ctx.getSelfMember();

        if (!selfMember.getVoiceState().inVoiceChannel()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I must be in a voice channel in order for this command to work!").build())
                    .queue();
            return;
        }

        GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();
        if (!memberVoiceState.inVoiceChannel()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command").build())
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfMember.getVoiceState().getChannel())) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command").build())
                    .queue();
            return;
        }

        if (filters.getRotation() != null) {
            filters.setRotation(null).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **off** the **8D** filter").build())
                    .queue();
        } else {
            filters.setRotation(new Rotation()
                    .setFrequency(0.05F)).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **on** the **8D** filter").build())
                    .queue();
        }
    }

    @Override
    public String getName() {
        return "8d";
    }

    @Override
    public String getHelp(String prefix) {
        return "Toggle the 8d filter";
    }
}
