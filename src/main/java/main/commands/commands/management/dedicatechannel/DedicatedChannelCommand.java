package main.commands.commands.management.dedicatechannel;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.requests.ErrorResponse;

import javax.script.ScriptException;
import java.util.List;

public class DedicatedChannelCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
            ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You do not have permission to execute this command")
                    .build()).queue();
            return;
        }

        final Message msg = ctx.getMessage();

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong())) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "The request channel has already been setup!").build())
                    .queue();
            return;
        }

        if (!ctx.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_CHANNEL)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                            I do not have enough permissions to do this!
                            Please give my role the `Manage Channels` permission in order for me to execute this command.

                            *For the recommended permissions please invite the bot using this link: https://bit.ly/3DfaNNl*""").build())
                    .queue();
            return;
        }

        guild.createTextChannel("robertify-requests").queue(
                textChannel -> {
                    var theme = new ThemesConfig().getTheme(guild.getIdLong());
                    var dediChannelConfig = new DedicatedChannelConfig();

                    ChannelManager manager = textChannel.getManager();
                    manager.setPosition(0).queue();
                    dediChannelConfig.channelTopicUpdateRequest(textChannel).queue();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(theme.getColor());
                    eb.setTitle("No song playing...");
                    eb.setImage(theme.getIdleBanner());
                    eb.setFooter("Prefix for this server is: " + new GuildConfig().getPrefix(guild.getIdLong()));


                    textChannel.sendMessage("**__Queue:__**\nJoin a voice channel and start playing songs!").setEmbeds(eb.build())
                            .queue(message -> {
                                dediChannelConfig.setChannelAndMessage(guild.getIdLong(), textChannel.getIdLong(), message.getIdLong());
                                dediChannelConfig.buttonUpdateRequest(message).queue();
                                dediChannelConfig.setOriginalAnnouncementToggle(guild.getIdLong(), new TogglesConfig().getToggle(guild, Toggles.ANNOUNCE_MESSAGES));

                                if ((RobertifyAudioManager.getInstance().getMusicManager(guild)).getPlayer().getPlayingTrack() != null)
                                    dediChannelConfig.updateMessage(guild);

                                msg.addReaction("✅").queue();
                            });


                },
                new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                                .queue())
                        .handle(ErrorResponse.MFA_NOT_ENABLED,
                                e -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I cannot execute this command because 2FA is required in this server!\n\n" +
                                        "*Tip: Try disabling 2FA temporarily and running the command again. After successful execution, you may turn 2FA on again.*").build()).queue())
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
        DISCONNECT(IDENTIFIER + "disconnect"),
        FAVOURITE(IDENTIFIER + "favourite");

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
