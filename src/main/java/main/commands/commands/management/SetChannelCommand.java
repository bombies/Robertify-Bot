package main.commands.commands.management;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.script.ScriptException;
import java.util.List;

public class SetChannelCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_ADMIN))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        BotUtils botUtils = new BotUtils();

        if (args.isEmpty()) {
            TextChannel channel = ctx.getChannel();
            if (botUtils.getAnnouncementChannel(guild.getIdLong()) == channel.getIdLong()) {
                EmbedBuilder eb = EmbedUtils.embedMessage("This is already the announcement channel.");
                msg.replyEmbeds(eb.build()).queue();
            } else {
                botUtils.createConnection();
                botUtils.setAnnouncementChannel(guild.getIdLong(), channel.getIdLong());

                EmbedBuilder eb = EmbedUtils.embedMessage("Set the announcement channel to: " + channel.getAsMention());
                msg.replyEmbeds(eb.build()).queue();
            }
        } else {
            String id = GeneralUtils.getDigitsOnly(args.get(0));

            if (!GeneralUtils.stringIsID(id)) {
                EmbedBuilder eb = EmbedUtils.embedMessage("ID passed isn't a valid ID!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            TextChannel channel = Robertify.api.getGuildById(guild.getIdLong()).getTextChannelById(id);

            if (channel == null) {
                EmbedBuilder eb = EmbedUtils.embedMessage("That ID doesn't belong to any channel in this guild!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            botUtils.setAnnouncementChannel(guild.getIdLong(), channel.getIdLong());

            EmbedBuilder eb =  EmbedUtils.embedMessage("You've set the announcement channel to: " +  channel.getAsMention());
            msg.replyEmbeds(eb.build()).queue();
        }

        botUtils.closeConnection();
    }

    @Override
    public String getName() {
        return "setchannel";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`" +
                "\nSet the announcement channel for when a new song is being played.\n\n" +
                "Usage: `"+ ServerUtils.getPrefix(Long.parseLong(guildID)) +"setchannel <channelID>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sc");
    }
}
