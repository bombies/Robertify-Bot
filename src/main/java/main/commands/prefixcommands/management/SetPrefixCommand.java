package main.commands.prefixcommands.management;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.guildconfig.GuildConfig;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

@Deprecated @ForRemoval
public class SetPrefixCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Guild guild = ctx.getGuild();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a prefix to set!");
            msg.replyEmbeds(eb.build()).queue();
        } else if (args.get(0).length() > 4) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Your prefix can't be more than 4 characters!");
            msg.replyEmbeds(eb.build()).queue();
        } else if (args.get(0).contains("`")) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Your prefix can't contain \"`\"");
            msg.replyEmbeds(eb.build()).queue();
        } else if (args.get(0).contains("/")) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Your prefix can't contain `/`").build()).queue();
        } else {
            new GuildConfig(guild).setPrefix(args.get(0));

            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You have set the bot's prefix to `" + args.get(0) + "`");
            msg.replyEmbeds(eb.build()).queue();
        }
    }

    @Override
    public String getName() {
        return "setprefix";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Sets the bot's prefix";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sp");
    }
}
