package main.commands.commands.audio.filters;

import lavalink.client.io.filters.Tremolo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;

import javax.script.ScriptException;

public class TremoloFilter implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        // TODO Paywall

        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var filters = audioPlayer.getFilters();

        if (filters.getTremolo() != null) {
            filters.setTremolo(null).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **off** the **Tremolo** filter").build())
                    .queue();
        } else {
            filters.setTremolo(new Tremolo()).commit();
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have turned **on** the **Tremolo** filter").build())
                    .queue();
        }
    }

    @Override
    public String getName() {
        return "tremolo";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
