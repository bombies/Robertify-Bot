package main.commands.commands.misc;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.script.ScriptException;
import java.sql.SQLException;
import java.util.List;

public class SetPrefixCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Guild guild = ctx.getGuild();
        final Message msg = ctx.getMessage();

        GeneralUtils.setCustomEmbed(BotConstants.ROBERTIFY_EMBED_TITLE + " | Set Prefix");

        if (args.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("You must provide a prefix to set!");
            msg.replyEmbeds(eb.build()).queue();
        } else if (args.get(0).length() > 4) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Your prefix can't be more than 4 characters!");
            msg.replyEmbeds(eb.build()).queue();
        } else if (args.get(0).contains("`")) {
            EmbedBuilder eb = EmbedUtils.embedMessage("Your prefix can't contain \"`\"");
            msg.replyEmbeds(eb.build()).queue();
        } else {
            ServerUtils serverUtils = new ServerUtils();

            serverUtils.updateServerPrefix(guild.getIdLong(), args.get(0)).closeConnection();

            EmbedBuilder eb = EmbedUtils.embedMessage("You have set the bot's prefix to `" + args.get(0) + "`");
            msg.replyEmbeds(eb.build()).queue();
        }
    }

    @Override
    public String getName() {
        return "setprefix";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("help");
    }
}
