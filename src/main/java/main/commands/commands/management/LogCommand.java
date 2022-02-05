package main.commands.commands.management;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class LogCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message message = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), main.constants.Permission.ROBERTIFY_ADMIN)) {
            message.replyEmbeds(
                    RobertifyEmbedUtils.embedMessage(
                            guild,
                            BotConstants.getInsufficientPermsMessage(main.constants.Permission.ROBERTIFY_ADMIN)
                    ).build())
                    .queue();
            return;
        }

        try {
            new LogUtils().createChannel(guild);
            message.addReaction("✅").queue();
        } catch (IllegalArgumentException e) {
            message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The logs channel has already been setup!").build())
                    .queue();
        }
    }

    @Override
    public String getName() {
        return "setuplogs";
    }

    @Override
    public String getHelp(String prefix) {
        return "Want to see every single action that's executed by users with regard to the music player?" +
                " This is the perfect command for you. Upon execution of this command, a log channel will " +
                "be created and all player updates will be sent to that channel which states the user who caused" +
                " the update. If you would like to remove this channel all you have to do is right click it and press " +
                "delete.";
    }

    @Override
    public List<Permission> getPermissionsRequired() {
        return List.of(Permission.MESSAGE_MANAGE, Permission.MANAGE_CHANNEL);
    }
}
