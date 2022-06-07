package main.commands.slashcommands.commands.management.dedicatedchannel;

import main.audiohandlers.RobertifyAudioManager;
import main.constants.Toggles;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class DedicatedChannelEditCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("requestchannel")
                        .setDescription("Configure the request channel for your server!")
                        .addSubCommands(
                                SubCommand.of(
                                        "setup",
                                        "Setup the request channel!"
                                ),
                                SubCommand.of(
                                        "edit",
                                        "Edit the request channel!"
                                )
                        )
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    protected void handleSetup(SlashCommandEvent event) {
        final var guild = event.getGuild();

        if (new DedicatedChannelConfig(guild).isChannelSet()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_ALREADY_SETUP).build())
                    .queue();
            return;
        }

        event.deferReply().queue();

        guild.createTextChannel("robertify-requests").queue(
                textChannel -> {
                    final var theme = new ThemesConfig(guild).getTheme();
                    final var dediChannelConfig = new DedicatedChannelConfig(guild);
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
                                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP, Pair.of("{channel}", textChannel.getAsMention())).build())
                                            .setEphemeral(false)
                                            .queue();
                                } catch (InsufficientPermissionException e) {
                                    if (e.getMessage().contains("MESSAGE_HISTORY"))
                                        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP_2).build())
                                                .queue();
                                    else e.printStackTrace();
                                }
                            });


                },
                new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                                .setEphemeral(true)
                                .queue())
        );
    }

    private void handleEdit(SlashCommandEvent event) {

    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        switch (event.getSubcommandName()) {
            case "setup" -> handleSetup(event);
            case "edit" -> handleEdit(event);
        }
    }
}
