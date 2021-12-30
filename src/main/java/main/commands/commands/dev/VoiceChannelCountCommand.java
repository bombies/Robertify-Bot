package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;
import me.duncte123.botcommons.messaging.EmbedUtils;

import javax.script.ScriptException;
import java.util.List;

public class VoiceChannelCountCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        int vcCount = 0;
        for (var guild : Robertify.api.getGuilds())
            vcCount += guild.getSelfMember().getVoiceState().inVoiceChannel() ? 1 : 0;

        ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage("🔊 I am currently in **" + vcCount + "** voice channels").build())
                .queue();
    }

    @Override
    public String getName() {
        return "voicechannelcount";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("vccount", "vcc");
    }
}
