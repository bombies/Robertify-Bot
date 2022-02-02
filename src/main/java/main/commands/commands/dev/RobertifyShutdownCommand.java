package main.commands.commands.dev;

import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.IDevCommand;
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

        for (Guild g : Robertify.api.getGuilds()) {
            var selfMember = g.getSelfMember();
            var musicManager = RobertifyAudioManager.getInstance().getMusicManager(g);

            if (selfMember.getVoiceState().inVoiceChannel())
                musicManager.leave();
        }

        try {
            FileUtils.cleanDirectory(new File(Config.get(ENV.AUDIO_DIR) + "/"));
        } catch (IllegalArgumentException ignored) {}

        JDA jda = Robertify.api;
        jda.shutdown();
        jda.getHttpClient().connectionPool().evictAll();
        jda.getHttpClient().dispatcher().executorService().shutdown();
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
