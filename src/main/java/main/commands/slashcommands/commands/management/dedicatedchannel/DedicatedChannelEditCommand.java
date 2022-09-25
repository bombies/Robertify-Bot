package main.commands.slashcommands.commands.management.dedicatedchannel;

import main.audiohandlers.RobertifyAudioManager;
import main.constants.Permission;
import main.constants.RobertifyEmoji;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

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

    protected void handleSetup(SlashCommandInteractionEvent event) {
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

    private void handleEdit(SlashCommandInteractionEvent event) {
        final var guild = event.getGuild();

        if (!GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_ADMIN)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_ADMIN.name())).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        DedicatedChannelConfig dedicatedChannelConfig = new DedicatedChannelConfig(guild);
        if (!dedicatedChannelConfig.isChannelSet()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_NOT_SET).build())
                    .queue();
            return;
        }

        event.deferReply().queue();

        final var localeManager = LocaleManager.getLocaleManager(guild);

        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_EDIT_EMBED)
                        .build())
                .addActionRow(
                        Button.primary("togglerqchannel:previous:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PREVIOUS)).withEmoji(RobertifyEmoji.PREVIOUS_EMOJI.getEmoji()),
                        Button.primary("togglerqchannel:rewind:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_REWIND)).withEmoji(RobertifyEmoji.REWIND_EMOJI.getEmoji()),
                        Button.primary("togglerqchannel:pnp:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PLAY_AND_PAUSE)).withEmoji(RobertifyEmoji.PLAY_AND_PAUSE_EMOJI.getEmoji()),
                        Button.primary("togglerqchannel:stop:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_STOP)).withEmoji(RobertifyEmoji.STOP_EMOJI.getEmoji()),
                        Button.primary("togglerqchannel:skip:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SKIP)).withEmoji(RobertifyEmoji.END_EMOJI.getEmoji())
                )
                .addActionRow(
                        Button.secondary("togglerqchannel:favourite:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_FAVOURITE)).withEmoji(RobertifyEmoji.STOP_EMOJI.getEmoji()),
                        Button.secondary("togglerqchannel:loop:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_LOOP)).withEmoji(RobertifyEmoji.LOOP_EMOJI.getEmoji()),
                        Button.secondary("togglerqchannel:shuffle:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SHUFFLE)).withEmoji(RobertifyEmoji.SHUFFLE_EMOJI.getEmoji()),
                        Button.secondary("togglerqchannel:disconnect:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_DISCONNECT)).withEmoji(RobertifyEmoji.QUIT_EMOJI.getEmoji()),
                        Button.secondary("togglerqchannel:filters:" + event.getUser().getId(), localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_FILTERS)).withEmoji(RobertifyEmoji.FILTER_EMOJI.getEmoji())
                )
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getButton().getId().startsWith("togglerqchannel:")) return;

        final var guild = event.getGuild();
        final var split = event.getButton().getId().split(":");
        if (!split[2].equals(event.getUser().getId())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_PERMS_BUTTON).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply().queue();

        final var localeManager = LocaleManager.getLocaleManager(guild);
        final var dedicatedChannelConfig = new DedicatedChannelConfig(guild);
        final var config = dedicatedChannelConfig.getConfig();
        final DedicatedChannelConfig.ChannelConfig.Field field;
        final String buttonName;
        switch (split[1]) {
            case "previous" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.PREVIOUS;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PREVIOUS);
            }
            case "rewind" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.REWIND;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_REWIND);
            }
            case "pnp" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.PLAY_PAUSE;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PLAY_AND_PAUSE);
            }
            case "stop" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.STOP;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_STOP);
            }
            case "skip" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.SKIP;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SKIP);
            }
            case "favourite" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.FAVOURITE;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_FAVOURITE);
            }
            case "loop" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.LOOP;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_LOOP);
            }
            case "shuffle" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.SHUFFLE;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SHUFFLE);
            }
            case "disconnect" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.DISCONNECT;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_DISCONNECT);
            }
            case "filters" -> {
                field = DedicatedChannelConfig.ChannelConfig.Field.FILTERS;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_FILTERS);
            }
            default -> throw new IllegalArgumentException("The button ID doesn't map to a case to be handled! ID: " + event.getButton().getId());
        }

        if (config.getState(field)) {
            config.setState(field, false);
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_BUTTON_TOGGLE,
                        Pair.of("{button}", buttonName),
                        Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                    ).build())
                    .queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS));
        } else {
            config.setState(field, true);
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_BUTTON_TOGGLE,
                            Pair.of("{button}", buttonName),
                            Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS).toUpperCase())
                    ).build())
                    .queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS));
        }

        dedicatedChannelConfig.updateButtons();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        switch (event.getSubcommandName()) {
            case "setup" -> handleSetup(event);
            case "edit" -> handleEdit(event);
        }
    }
}
