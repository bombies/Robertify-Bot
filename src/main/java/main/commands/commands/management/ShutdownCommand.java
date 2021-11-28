package main.commands.commands.management;

import lombok.SneakyThrows;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.utils.database.BotDB;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.BotCommons;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;

import javax.script.ScriptException;

public class ShutdownCommand implements IDevCommand {
    @SneakyThrows
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        EmbedBuilder eb = EmbedUtils.embedMessage("Now shutting down...");
        ctx.getMessage().replyEmbeds(eb.build()).queue();

        for (Guild g : new BotDB().getGuilds()) {
            var selfMember = g.getSelfMember();
            var musicManager = PlayerManager.getInstance().getMusicManager(g);
            var queue = musicManager.scheduler.queue;
            var audioPlayer = musicManager.audioPlayer;

            if (audioPlayer.getPlayingTrack() != null)
                audioPlayer.stopTrack();

            if (!queue.isEmpty())
                queue.clear();

            if (selfMember.getVoiceState().inVoiceChannel())
                g.getAudioManager().closeAudioConnection();

            if (new DedicatedChannelConfig().isChannelSet(g.getId()))
                new DedicatedChannelConfig().updateMessage(g);
        }

        BotCommons.shutdown(ctx.getJDA());
        System.exit(1000);
    }

    @Override
    public String getName() {
        return "shutdown";
    }

    @Override
    public String getHelp(String guildID) {
        return "Shuts the bot down";
    }
}
