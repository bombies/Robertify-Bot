package main.commands.commands.management.dedicatechannel;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.commands.commands.management.toggles.togglesconfig.Toggles;
import main.commands.commands.management.toggles.togglesconfig.TogglesConfig;
import main.constants.ENV;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.ServerDB;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.requests.ErrorResponse;

import javax.script.ScriptException;
import java.util.List;

public class DedicatedChannelCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_ADMIN)) {
            ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage("You do not have permission to execute this command")
                    .build()).queue();
            return;
        }

        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        if (new DedicatedChannelConfig().isChannelSet(guild.getId())) {
            msg.replyEmbeds(EmbedUtils.embedMessage("The request channel has already been setup!").build())
                    .queue();
            return;
        }

        if (!ctx.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_CHANNEL)) {
            msg.replyEmbeds(EmbedUtils.embedMessage("""
                            I do not have enough permissions to do this!
                            Please give my role the `Manage Channels` permission in order for me to execute this command.

                            *For the recommended permissions please invite the bot using this link: https://bit.ly/3DfaNNl*""").build())
                    .queue();
            return;
        }

        guild.createTextChannel("robertify-requests").queue(
                textChannel -> {
                    var dediChannelConfig = new DedicatedChannelConfig();

                    ChannelManager manager = textChannel.getManager();
                    manager.setPosition(0).queue();
                    dediChannelConfig.channelTopicUpdateRequest(textChannel).queue();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(GeneralUtils.parseColor(Config.get(ENV.BOT_COLOR)));
                    eb.setTitle("No song playing...");
                    eb.setImage("https://i.imgur.com/1HDoSgP.png");
                    eb.setFooter("Prefix for this server is: " + ServerDB.getPrefix(guild.getIdLong()));


                    textChannel.sendMessage("**__Queue:__**\nJoin a voice channel and start playing songs!").setEmbeds(eb.build())
                            .queue(message -> {
                                dediChannelConfig.setChannelAndMessage(guild.getId(), textChannel.getId(), message.getId());
                                dediChannelConfig.buttonUpdateRequest(message).queue();
                                dediChannelConfig.setOriginalAnnouncementToggle(guild.getId(), new TogglesConfig().getToggle(guild, Toggles.ANNOUNCE_MESSAGES));
                            });

                    msg.addReaction("✅").queue();
                },
                new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            msg.replyEmbeds(EmbedUtils.embedMessage(e.getMessage()).build())
                                    .queue();
                        })
        );

    }

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getHelp(String prefix) {
        return "Running this command will build a channel in which you can easily control the bot and" +
                " queue songs. When this channel is created, if you want it removed all you have to do" +
                " is right click on it and delete it. Once the channel is created you can find it at the" +
                " top of your channel list. Happy listening!";
    }

    @Override
    public List<net.dv8tion.jda.api.Permission> getPermissionsRequired() {
        return List.of(net.dv8tion.jda.api.Permission.MESSAGE_MANAGE,
                net.dv8tion.jda.api.Permission.MANAGE_CHANNEL);
    }

    public enum ButtonID {
        IDENTIFIER("dedicated"),
        PREVIOUS(IDENTIFIER + "previous"),
        REWIND(IDENTIFIER + "rewind"),
        PLAY_AND_PAUSE(IDENTIFIER + "pnp"),
        STOP(IDENTIFIER  + "stop"),
        END(IDENTIFIER + "end"),
        LOOP(IDENTIFIER + "loop"),
        SHUFFLE(IDENTIFIER + "shuffle"),
        DISCONNECT(IDENTIFIER + "disconnect");

        private final String str;

        ButtonID(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
