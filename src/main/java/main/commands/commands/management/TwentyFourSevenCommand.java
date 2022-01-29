package main.commands.commands.management;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.script.ScriptException;

public class TwentyFourSevenCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        // TODO Add paywall logic

        final var guild = ctx.getGuild();
        final var msg = ctx.getMessage();

        msg.replyEmbeds(logic(guild)).queue();
    }

    private MessageEmbed logic(Guild guild) {
        final var config = new GuildConfig();
        
        if (config.get247(guild.getIdLong())) {
            config.set247(guild.getIdLong(), false);

            RobertifyAudioManager.getInstance().getMusicManager(guild)
                    .getScheduler().scheduleDisconnect(true);

            return RobertifyEmbedUtils.embedMessage(guild, "You have turned 24/7 mode **off**").build();
        } else {
            config.set247(guild.getIdLong(), true);
            return RobertifyEmbedUtils.embedMessage(guild, "You have turned 24/7 mode **on**").build();
        }
    }

    @Override
    public String getName() {
        return "24/7";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
