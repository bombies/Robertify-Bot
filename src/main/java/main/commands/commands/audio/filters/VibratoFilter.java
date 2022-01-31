package main.commands.commands.audio.filters;

import lavalink.client.io.filters.Vibrato;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;

import javax.script.ScriptException;

public class VibratoFilter implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        // TODO Paywall

        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var filters = audioPlayer.getFilters();

        if (filters.getVibrato() != null) {
            filters.setVibrato(null).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **off** the **Vibrato** filter").build())
                    .queue();
        } else {
            filters.setVibrato(new Vibrato()).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **on** the **Vibrato** filter").build())
                    .queue();
        }
    }

    @Override
    public String getName() {
        return "vibrato";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
