package main.commands.slashcommands.commands.management.requestchannel;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class RequestChannelCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();

        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
            ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS_NO_ARGS)
                    .build()).queue();
            return;
        }

        final Message msg = ctx.getMessage();

        if (new RequestChannelConfig(guild).isChannelSet()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_ALREADY_SETUP).build())
                    .queue();
            return;
        }

        if (!ctx.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_CHANNEL)) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SELF_INSUFFICIENT_PERMS_ARGS, Pair.of("{permissions}", "`Manage Channels`")).build())
                    .queue();
            return;
        }

        guild.createTextChannel("robertify-requests").queue(
                textChannel -> {
                    final var theme = new ThemesConfig(guild).getTheme();
                    final var dediChannelConfig = new RequestChannelConfig(guild);
                    final var localeManager = LocaleManager.getLocaleManager(guild);
                    final var manager = textChannel.getManager();

                    manager.setPosition(0).queue();
                    dediChannelConfig.channelTopicUpdateRequest(textChannel).queue();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(theme.getColor());
                    eb.setTitle(localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_NOTHING_PLAYING));
                    eb.setImage(theme.getIdleBanner());

                    textChannel.sendMessage(localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING)).setEmbeds(eb.build())
                            .queue(message -> {
                                dediChannelConfig.setChannelAndMessage(textChannel.getIdLong(), message.getIdLong());
                                dediChannelConfig.buttonUpdateRequest(message).queue();
                                dediChannelConfig.setOriginalAnnouncementToggle(new TogglesConfig(guild).getToggle(Toggles.ANNOUNCE_MESSAGES));

                                if ((RobertifyAudioManager.getInstance().getMusicManager(guild)).getPlayer().getPlayingTrack() != null)
                                    dediChannelConfig.updateMessage();

                                try {
                                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP, Pair.of("{channel}", textChannel.getAsMention())).build())
                                                    .queue();
                                } catch (InsufficientPermissionException e) {
                                    if (e.getMessage().contains("MESSAGE_HISTORY"))
                                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP_2).build())
                                                .queue();
                                    else e.printStackTrace();
                                }
                            });
                },
                new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                                .queue())
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

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("setup")
                        .setDescription("Create the easy-to-use requests channel!")
                        .setAdminOnly()
                        .setBotRequiredPermissions(
                                net.dv8tion.jda.api.Permission.MESSAGE_MANAGE,
                                net.dv8tion.jda.api.Permission.MANAGE_CHANNEL
                        )
                        .build()
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        new RequestChannelEditCommand().handleSetup(event);
    }

    @Override
    public String getHelp() {
        return "Running this command will build a channel in which you can easily control the bot and" +
                " queue songs. When this channel is created, if you want it removed all you have to do" +
                " is right click on it and delete it. Once the channel is created you can find it at the" +
                " top of your channel list. Happy listening!";
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
