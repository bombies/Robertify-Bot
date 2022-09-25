package main.commands.slashcommands.commands.management;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class LogCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message message = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), main.constants.Permission.ROBERTIFY_ADMIN)) {
            message.replyEmbeds(
                    RobertifyEmbedUtils.embedMessage(
                            guild,
                            BotConstants.getInsufficientPermsMessage(guild, main.constants.Permission.ROBERTIFY_ADMIN)
                    ).build())
                    .queue();
            return;
        }

        try {
            new LogUtils(guild).createChannel();
            message.addReaction(Emoji.fromFormatted("✅")).queue();
        } catch (IllegalArgumentException e) {
            message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.LOG_CHANNEL_ALREADY_SETUP).build())
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

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("setuplogs")
                        .setDescription("Setup the log channel for all Robertify player actions!")
                        .setBotRequiredPermissions(
                                Permission.MESSAGE_MANAGE,
                                Permission.MANAGE_CHANNEL
                        )
                        .setAdminOnly()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Want to see every single action that's executed by users with regard to the music player?" +
                " This is the perfect command for you. Upon execution of this command, a log channel will " +
                "be created and all player updates will be sent to that channel which states the user who caused" +
                " the update. If you would like to remove this channel all you have to do is right click it and press " +
                "delete.";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final Guild guild = event.getGuild();

        try {
            new LogUtils(guild).createChannel();
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.LOG_CHANNEL_SUCCESSFUL_SETUP).build())
                    .queue();
        } catch (IllegalArgumentException e) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LogChannelMessages.LOG_CHANNEL_ALREADY_SETUP).build())
                    .queue();
        }
    }
}
