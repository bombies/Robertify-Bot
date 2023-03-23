package main.commands.slashcommands.commands.management.requestchannel;

import lombok.extern.slf4j.Slf4j;
import main.constants.Permission;
import main.constants.RobertifyEmoji;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RequestChannelEditCommand extends AbstractSlashCommand {
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

        if (new RequestChannelConfig(guild).isChannelSet()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_ALREADY_SETUP).build())
                    .queue();
            return;
        }
        event.deferReply().queue();
        new RequestChannelCommand().createRequestChannel(event.getGuild())
                .thenAccept(channel -> {
                    try {
                        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP, Pair.of("{channel}", GeneralUtils.toMention(guild, channel.getChannelId(), GeneralUtils.Mentioner.CHANNEL))).build())
                                .queue();
                    } catch (InsufficientPermissionException e) {
                        if (e.getMessage().contains("MESSAGE_HISTORY"))
                            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP_2).build())
                                    .queue();
                        else log.error("Unexpected error", e);
                    }
                });
    }

    private void handleEdit(SlashCommandInteractionEvent event) {
        final var guild = event.getGuild();

        if (!GeneralUtils.hasPerms(guild, event.getMember(), Permission.ROBERTIFY_ADMIN)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_ADMIN.name())).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        RequestChannelConfig requestChannelConfig = new RequestChannelConfig(guild);
        if (!requestChannelConfig.isChannelSet()) {
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
        handleChannelButtonToggle(guild, split[1], event);
    }
    
    public void handleChannelButtonToggle(Guild guild, String button, @Nullable ButtonInteractionEvent event) {
        final LocaleManager localeManager = LocaleManager.getLocaleManager(guild);
        final var dedicatedChannelConfig = new RequestChannelConfig(guild);
        final var config = dedicatedChannelConfig.getConfig();
        final RequestChannelConfig.ChannelConfig.Field field;
        final String buttonName;
        
        switch (button) {
            case "previous" -> {
                field = RequestChannelConfig.ChannelConfig.Field.PREVIOUS;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PREVIOUS);
            }
            case "rewind" -> {
                field = RequestChannelConfig.ChannelConfig.Field.REWIND;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_REWIND);
            }
            case "pnp" -> {
                field = RequestChannelConfig.ChannelConfig.Field.PLAY_PAUSE;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PLAY_AND_PAUSE);
            }
            case "stop" -> {
                field = RequestChannelConfig.ChannelConfig.Field.STOP;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_STOP);
            }
            case "skip" -> {
                field = RequestChannelConfig.ChannelConfig.Field.SKIP;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SKIP);
            }
            case "favourite" -> {
                field = RequestChannelConfig.ChannelConfig.Field.FAVOURITE;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_FAVOURITE);
            }
            case "loop" -> {
                field = RequestChannelConfig.ChannelConfig.Field.LOOP;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_LOOP);
            }
            case "shuffle" -> {
                field = RequestChannelConfig.ChannelConfig.Field.SHUFFLE;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SHUFFLE);
            }
            case "disconnect" -> {
                field = RequestChannelConfig.ChannelConfig.Field.DISCONNECT;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_DISCONNECT);
            }
            case "filters" -> {
                field = RequestChannelConfig.ChannelConfig.Field.FILTERS;
                buttonName = localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_FILTERS);
            }
            default -> throw new IllegalArgumentException("The button ID doesn't map to a case to be handled! ID: " + event.getButton().getId());
        }

        if (config.getState(field)) {
            config.setState(field, false);
            if (event != null)
                event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_BUTTON_TOGGLE,
                                Pair.of("{button}", buttonName),
                                Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS).toUpperCase())
                        ).build())
                        .queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS));
        } else {
            config.setState(field, true);
            if (event != null)
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
