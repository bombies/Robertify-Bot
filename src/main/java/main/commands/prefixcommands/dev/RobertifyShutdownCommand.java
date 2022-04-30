package main.commands.prefixcommands.dev;

import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.commons.io.FileUtils;

import javax.script.ScriptException;
import java.io.File;

public class RobertifyShutdownCommand implements IDevCommand {
    @SneakyThrows
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!ctx.getAuthor().getId().equals(Config.get(ENV.OWNER_ID)))
            return;

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "Now shutting down...");
        ctx.getMessage().replyEmbeds(eb.build()).queue();

        for (Guild g : Robertify.shardManager.getGuilds()) {
            var selfMember = g.getSelfMember();
            var musicManager = RobertifyAudioManager.getInstance().getMusicManager(g);

            if (selfMember.getVoiceState().inAudioChannel())
                musicManager.leave();
        }

        try {
            FileUtils.cleanDirectory(new File(Config.get(ENV.AUDIO_DIR) + "/"));
        } catch (IllegalArgumentException ignored) {}

        final var shardManager = Robertify.shardManager;
        shardManager.shutdown();
        WebUtils.ins.shutdown();

        System.exit(1000);
    }

    @Override
    public String getName() {
        return "shutdown";
    }

    @Override
    public String getHelp(String prefix) {
        return "Shuts the bot down";
    }
}
